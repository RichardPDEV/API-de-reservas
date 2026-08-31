import React, { useEffect, useState } from "react";
import { INITIAL_RESTAURANTS } from "./lib/data.js";
import { API_BASE_URL } from "./lib/constants.js";
import { clearAccessToken, getAccessToken, requestJson, setAccessToken } from "./lib/api.js";
import {
  readAccounts,
  writeAccounts,
  readRegisteredRestaurants,
  writeRegisteredRestaurants,
  readRestaurantSession,
  writeRestaurantSession,
  writeClientSession,
} from "./lib/storage.js";
import { mapRestaurantToBackend, loadPublicRestaurants, buildIsoDateTime, getEndTimeFromStart } from "./lib/restaurantBackend.js";
import { normalizeRestaurantLayout, sameRestaurantId } from "./lib/layout.js";
import ErrorBoundary from "./components/ErrorBoundary.jsx";
import LandingPage from "./pages/LandingPage.jsx";
import ClientAuthGate from "./pages/ClientAuthGate.jsx";
import ClientHome from "./pages/ClientHome.jsx";
import ClientReservation from "./pages/ClientReservation.jsx";
import RestaurantAuth from "./pages/RestaurantAuth.jsx";
import RestaurantDashboard from "./pages/RestaurantDashboard.jsx";

const dedupeRestaurants = (items = []) => {
  const seen = new Map();

  for (const entry of items) {
    const restaurant = normalizeRestaurantLayout(entry);
    if (!restaurant) continue;

    const backendKey = restaurant.backendBusinessId ? `backend:${restaurant.backendBusinessId}` : null;
    const localKey = restaurant.id != null ? `local:${restaurant.id}` : null;
    const fallbackKey = restaurant.name && restaurant.address ? `name:${restaurant.name}|${restaurant.address}` : null;
    const key = backendKey || localKey || fallbackKey;
    if (!key) continue;

    const current = seen.get(key);
    if (!current) {
      seen.set(key, restaurant);
      continue;
    }

    seen.set(key, {
      ...current,
      ...restaurant,
      tables: restaurant.tables?.length ? restaurant.tables : current.tables || [],
      layoutElements: restaurant.layoutElements?.length ? restaurant.layoutElements : current.layoutElements || [],
      reservations: restaurant.reservations?.length ? restaurant.reservations : current.reservations || [],
      floorNames: restaurant.floorNames || current.floorNames || { 1: "Piso principal" },
      floorCount: Math.max(Number(restaurant.floorCount) || 1, Number(current.floorCount) || 1),
    });
  }

  return [...seen.values()];
};

export default function App() {
  const [restaurants, setRestaurants] = useState(INITIAL_RESTAURANTS);
  const [view, setView] = useState("landing"); // landing | client-home | client-reserve | restaurant-auth | restaurant-dash
  const [selectedRestaurant, setSelectedRestaurant] = useState(null);
  const [backendStatus, setBackendStatus] = useState("loading");
  const [restaurantSession, setRestaurantSession] = useState(readRestaurantSession());
  const [authError, setAuthError] = useState("");

  useEffect(() => {
    let isMounted = true;

    async function loadRestaurants() {
      const registered = dedupeRestaurants(readRegisteredRestaurants().map(normalizeRestaurantLayout).filter(Boolean));
      const combined = dedupeRestaurants([...INITIAL_RESTAURANTS, ...registered]);
      let publicRestaurants = [];

      try {
        publicRestaurants = dedupeRestaurants(await loadPublicRestaurants());
      } catch (error) {
        console.warn("No se pudieron cargar los restaurantes públicos:", error);
      }

      try {
        const liveRestaurants = dedupeRestaurants(await Promise.all(combined.map(mapRestaurantToBackend)));
        const mergedRestaurants = dedupeRestaurants([...liveRestaurants, ...publicRestaurants]);
        if (isMounted) {
          setRestaurants(mergedRestaurants);
          setBackendStatus("connected");
        }
      } catch (error) {
        if (isMounted) {
          setRestaurants(combined);
          setBackendStatus("fallback");
          console.error("No se pudo conectar con el backend:", error);
        }
      }
    }

    loadRestaurants();
    return () => {
      isMounted = false;
    };
  }, []);

  const syncRestaurantSession = (session) => {
    setRestaurantSession(session);
    writeRestaurantSession(session);
  };

  useEffect(() => {
    if (!restaurantSession) {
      clearAccessToken();
      return;
    }

    if (restaurantSession?.token) {
      setAccessToken(restaurantSession.token);
      return;
    }

    async function refreshRestaurantAuth() {
      try {
        const profile = await requestJson(`${API_BASE_URL}/auth/me`);
        if (profile?.role && profile.role !== "OWNER") {
          throw new Error("La sesión activa pertenece a un cliente");
        }
      } catch (err) {
        console.warn("No se pudo refrescar la sesión del restaurante:", err);
        clearAccessToken();
        syncRestaurantSession(null);
      }
    }

    refreshRestaurantAuth();
  }, [restaurantSession]);

  const handleRestaurantRegister = async (form) => {
    const { name, cuisine, address, phone, email, password, description } = form;
    if (!name || !email || !password || !address) {
      throw new Error("Completa los campos obligatorios");
    }

    const accounts = readAccounts().filter(
      (account) => account.email?.trim().toLowerCase() !== email
    );
    let businessId = null;
    let resourceId = null;
    let registrationError = null;

    try {
      const business = await requestJson(`${API_BASE_URL}/v1/businesses`, {
        method: "POST",
        body: JSON.stringify({
          name,
          type: "RESTAURANT",
          cuisine,
          address,
          phone,
          description,
          tableLayoutJson: JSON.stringify([]),
        }),
      });
      businessId = business.id;

      const resource = await requestJson(`${API_BASE_URL}/v1/businesses/${business.id}/resources`, {
        method: "POST",
        body: JSON.stringify({ businessId: business.id, name: `${name} mesa`, capacity: 8 }),
      });
      resourceId = resource.id;
    } catch (error) {
      console.warn("No se pudo crear el negocio/resource en el backend:", error);
      registrationError = error;
    }

    const restaurant = normalizeRestaurantLayout({
      id: Date.now(),
      name,
      cuisine,
      address,
      phone,
      description,
      image: "🍴",
      tables: [],
      reservations: [],
      openTime: "12:00",
      closeTime: "23:00",
      backendBusinessId: businessId,
      backendResourceId: resourceId,
    });

    writeAccounts([...accounts, { email, password, restaurantId: restaurant.id, businessId, resourceId }]);
    const mergedRegistered = dedupeRestaurants([...readRegisteredRestaurants(), restaurant]);
    writeRegisteredRestaurants(mergedRegistered);
    setRestaurants((prev) => dedupeRestaurants([...prev, restaurant]));

    const session = { email, restaurantId: restaurant.id, businessId, resourceId, token: getAccessToken() };
    writeClientSession(null);
    syncRestaurantSession(session);
    setAuthError("");
    setView("restaurant-dash");

    if (registrationError) {
      console.warn("Registro completado localmente, pero hubo un problema en backend:", registrationError);
    }
  };

  const handleRestaurantLogin = async ({ email, password }) => {
    const normalizedEmail = (email || "").trim().toLowerCase();
    const loginResp = await requestJson(`${API_BASE_URL}/auth/login`, {
      method: "POST",
      body: JSON.stringify({ username: normalizedEmail, password }),
    });

    const token = loginResp?.token;
    if (!token) {
      throw new Error("No se pudo iniciar sesión en el backend del restaurante");
    }
    if (loginResp?.role && loginResp.role !== "OWNER") {
      clearAccessToken();
      throw new Error("Esta cuenta pertenece a un cliente");
    }
    setAccessToken(token);

    const accounts = readAccounts();
    const account = accounts.find((item) => item.email?.trim().toLowerCase() === normalizedEmail);

    const registeredRestaurants = readRegisteredRestaurants();
    const restaurant = account
      ? normalizeRestaurantLayout(
          registeredRestaurants.find((item) => sameRestaurantId(item.id, account.restaurantId))
        )
      : null;
    if (restaurant && !restaurants.some((r) => sameRestaurantId(r.id, restaurant.id))) {
      setRestaurants((prev) => [...prev, restaurant]);
    }

    const session = {
      email: normalizedEmail,
      restaurantId: account?.restaurantId ?? null,
      businessId: account?.businessId ?? null,
      resourceId: account?.resourceId ?? null,
      token,
    };
    writeClientSession(null);
    syncRestaurantSession(session);
    setAuthError("");
    setView("restaurant-dash");
  };

  const handleConfirmReservation = async (data) => {
    const restaurantId = selectedRestaurant?.id;
    const restaurant = restaurants.find((r) => r.id === restaurantId);
    if (!restaurant) return;

    const restaurantWithBackend = restaurant.backendResourceId
      ? restaurant
      : await mapRestaurantToBackend(restaurant);

    const resourceId = restaurantWithBackend?.backendResourceId || restaurantWithBackend?.resourceId;
    if (!resourceId) {
      throw new Error("No se pudo preparar el restaurante para reservar");
    }

    const startTime = buildIsoDateTime(data.date, data.time);
    const payload = {
      resourceId: data.resourceId || resourceId,
      tableId: data.tableId,
      customerName: data.name,
      customerEmail: data.email,
      partySize: data.guests,
      startTime,
      endTime: getEndTimeFromStart(startTime, 2),
    };

    try {
      const created = await requestJson(`${API_BASE_URL}/v1/reservations`, {
        method: "POST",
        body: JSON.stringify(payload),
      });

      setRestaurants((prev) =>
        prev.map((r) => {
          if (r.id !== restaurantId) return r;
          return {
            ...r,
            ...restaurantWithBackend,
            tables: (restaurantWithBackend.tables || r.tables).map((t) => (t.id === data.tableId ? { ...t, status: "reserved" } : t)),
            reservations: [
              ...r.reservations,
              {
                id: created?.id?.toString() || `R${Date.now()}`,
                tableId: data.tableId,
                date: data.date,
                time: data.time,
                name: data.name,
                guests: data.guests,
                status: "confirmed",
              },
            ],
          };
        })
      );
      setBackendStatus("connected");
      return created;
    } catch (error) {
      console.error("Reserva en el backend falló:", error);
      setBackendStatus("fallback");
      throw error;
    }
  };

  const logoutRestaurant = async () => {
    try {
      await requestJson(`${API_BASE_URL}/auth/logout`, { method: "POST", skipRefresh: true });
    } catch (err) {
      console.warn("Logout restaurant failed", err);
    }
    clearAccessToken();
    syncRestaurantSession(null);
    setView("landing");
  };

  const deleteRestaurantAccount = async () => {
    const session = restaurantSession;
    if (!session || !session.businessId) {
      clearAccessToken();
      syncRestaurantSession(null);
      setView("landing");
      return;
    }

    const confirmed = window.confirm("¿Seguro que quieres borrar este restaurante y su cuenta? Esta acción no se puede deshacer.");
    if (!confirmed) return;

    try {
      await requestJson(`${API_BASE_URL}/v1/businesses/${session.businessId}`, {
        method: "DELETE",
      });

      const accounts = readAccounts().filter((account) => account.email?.trim().toLowerCase() !== (session.email || "").trim().toLowerCase());
      writeAccounts(accounts);
      const registered = dedupeRestaurants(readRegisteredRestaurants()).filter(
        (restaurant) => !sameRestaurantId(restaurant.id, session.restaurantId) && !sameRestaurantId(restaurant.backendBusinessId, session.businessId)
      );
      writeRegisteredRestaurants(registered);
      clearAccessToken();
      syncRestaurantSession(null);
      setRestaurants((prev) => dedupeRestaurants(prev).filter(
        (restaurant) => !sameRestaurantId(restaurant.id, session.restaurantId) && !sameRestaurantId(restaurant.backendBusinessId, session.businessId)
      ));
      setAuthError("");
      setView("landing");
    } catch (error) {
      console.error("No se pudo borrar el restaurante:", error);
      throw new Error(error?.message || "No se pudo borrar el restaurante");
    }
  };

  const handleSaveRestaurant = (updatedRestaurant) => {
    setRestaurants((prev) => prev.map((r) => (r.id === updatedRestaurant.id ? updatedRestaurant : r)));
    const registered = dedupeRestaurants(readRegisteredRestaurants())
      .map((r) => (sameRestaurantId(r.id, updatedRestaurant.id) ? updatedRestaurant : r));
    writeRegisteredRestaurants(dedupeRestaurants(registered));
  };

  let content;
  if (view === "landing") {
    content = <LandingPage onEnterClient={() => setView("client-home")} onEnterRestaurant={() => setView("restaurant-auth")} />;
  } else if (view === "client-auth") {
    content = <ClientAuthGate onBack={() => setView("landing")} onContinue={() => setView("client-home")} />;
  } else if (view === "client-home") {
    content = (
      <ClientHome
        restaurants={restaurants}
        onSelectRestaurant={(r) => { setSelectedRestaurant(r); setView("client-reserve"); }}
        onBack={() => setView("landing")}
      />
    );
  } else if (view === "client-reserve") {
    content = (
      <ClientReservation
        restaurant={normalizeRestaurantLayout(
          restaurants.find((r) => sameRestaurantId(r.id, selectedRestaurant?.id)) || selectedRestaurant
        )}
        onBack={() => setView("client-home")}
        onConfirm={handleConfirmReservation}
      />
    );
  } else if (view === "restaurant-auth") {
    content = (
      <RestaurantAuth
        onRegister={handleRestaurantRegister}
        onLogin={handleRestaurantLogin}
        onBack={() => setView("landing")}
        errorMessage={authError}
      />
    );
  } else if (view === "restaurant-dash") {
    content = (
      <RestaurantDashboard
        restaurants={restaurants}
        initialRestaurantId={restaurantSession?.restaurantId}
        onBack={() => setView("landing")}
        onLogout={logoutRestaurant}
        onDeleteRestaurant={deleteRestaurantAccount}
        onSaveRestaurant={handleSaveRestaurant}
      />
    );
  }

  return (
    <ErrorBoundary key={view}>
      <div style={{ minHeight: "100vh" }}>
        {content}
      </div>
    </ErrorBoundary>
  );
}
