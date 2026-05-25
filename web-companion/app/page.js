'use client';

import { useState, useEffect, useRef, useCallback } from 'react';

// ── Toast system ──────────────────────────────────────────────────────────────
// Types: 'success' | 'error' | 'warning' | 'info'
function useToasts() {
  const [toasts, setToasts] = useState([]);

  const addToast = useCallback((message, type = 'info') => {
    const id = `${Date.now()}_${Math.random()}`;
    setToasts(prev => [...prev, { id, message, type }]);
    setTimeout(() => {
      setToasts(prev => prev.filter(t => t.id !== id));
    }, 4000);
  }, []);

  const removeToast = useCallback((id) => {
    setToasts(prev => prev.filter(t => t.id !== id));
  }, []);

  return { toasts, addToast, removeToast };
}

function ToastContainer({ toasts, onRemove }) {
  const iconMap = {
    success: '✓',
    error: '✕',
    warning: '⚠',
    info: 'ℹ',
  };
  const colorMap = {
    success: { bg: 'rgba(102,187,106,0.12)', border: 'rgba(102,187,106,0.3)', text: '#66bb6a' },
    error: { bg: 'rgba(239,83,80,0.12)', border: 'rgba(239,83,80,0.3)', text: '#ef5350' },
    warning: { bg: 'rgba(255,152,0,0.12)', border: 'rgba(255,152,0,0.3)', text: '#ff9800' },
    info: { bg: 'rgba(124,131,253,0.12)', border: 'rgba(124,131,253,0.3)', text: '#7c83fd' },
  };

  return (
    <div className="toast-container-mobile" style={{ position: 'fixed', bottom: 24, right: 16, left: 16, display: 'flex', flexDirection: 'column', gap: '10px', zIndex: 9999, maxWidth: '420px', margin: '0 auto' }}>
      {toasts.map(toast => {
        const colors = colorMap[toast.type] || colorMap.info;
        return (
          <div
            key={toast.id}
            style={{
              display: 'flex', alignItems: 'center', gap: '12px',
              background: 'var(--bg-elevated)',
              border: `1px solid ${colors.border}`,
              borderLeft: `4px solid ${colors.text}`,
              color: 'var(--text-primary)',
              padding: '12px 16px',
              borderRadius: '10px',
              fontSize: '13px',
              fontWeight: '600',
              boxShadow: '0 4px 20px rgba(0,0,0,0.4)',
              animation: 'toast-in 0.3s ease',
              cursor: 'pointer',
            }}
            onClick={() => onRemove(toast.id)}
          >
            <span style={{ color: colors.text, fontSize: '15px', flexShrink: 0 }}>{iconMap[toast.type]}</span>
            <span style={{ flex: 1 }}>{toast.message}</span>
          </div>
        );
      })}
    </div>
  );
}

// ── Login Screen ──────────────────────────────────────────────────────────────
function LoginScreen({ onLogin }) {
  const [isLoginTab, setIsLoginTab] = useState(true);
  const [loginUser, setLoginUser] = useState('');
  const [loginCode, setLoginCode] = useState('');
  const [regUser, setRegUser] = useState('');
  const [regCode, setRegCode] = useState('');
  const [regMembers, setRegMembers] = useState('');
  const [errorMsg, setErrorMsg] = useState(null);
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    if (!isLoginTab && !regCode) {
      setRegCode(`HOGAR-${Math.floor(Math.random() * 9000) + 1000}`);
    }
    setErrorMsg(null);
  }, [isLoginTab]);

  const handleLogin = async () => {
    const user = loginUser.trim();
    const code = loginCode.trim().toUpperCase();
    if (!user) { setErrorMsg('Por favor, ingresa tu nombre de miembro.'); return; }
    if (!code) { setErrorMsg('Se requiere el código del hogar.'); return; }
    setIsLoading(true);
    await new Promise(r => setTimeout(r, 600));
    onLogin(user, code, false);
    setIsLoading(false);
  };

  const handleRegister = async () => {
    const user = regUser.trim();
    const code = regCode.trim().toUpperCase();
    if (!user) { setErrorMsg('El nombre de administrador es requerido.'); return; }
    if (!code) { setErrorMsg('El código propuesto del hogar no puede quedar vacío.'); return; }
    setIsLoading(true);
    await new Promise(r => setTimeout(r, 600));
    onLogin(user, code, true);
    setIsLoading(false);
  };

  return (
    <div style={{
      minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center',
      background: 'var(--bg-base)', padding: '24px',
    }}>
      <div style={{ width: '100%', maxWidth: '420px', display: 'flex', flexDirection: 'column', gap: '20px' }}>
        {/* Logo */}
        <div style={{ textAlign: 'center' }}>
          <div style={{
            width: '64px', height: '64px', borderRadius: '18px',
            background: 'var(--neon-dim)', border: '1px solid var(--neon-border)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            margin: '0 auto 16px',
          }}>
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="var(--neon)" strokeWidth="2">
              <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
              <polyline points="9 22 9 12 15 12 15 22"/>
            </svg>
          </div>
          <h1 style={{ fontSize: '26px', fontWeight: '800', color: 'var(--neon)', margin: 0 }}>Hogar Sincro</h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '13px', marginTop: '4px' }}>Consola Web Inteligente</p>
        </div>

        {/* Tab switcher */}
        <div style={{
          display: 'grid', gridTemplateColumns: '1fr 1fr',
          background: 'var(--bg-elevated)', borderRadius: '10px',
          padding: '4px', border: '1px solid var(--border)',
        }}>
          {['Iniciar Sesión', 'Registrar Hogar'].map((label, i) => (
            <button
              key={label}
              onClick={() => setIsLoginTab(i === 0)}
              style={{
                padding: '9px', borderRadius: '7px', border: 'none', cursor: 'pointer',
                fontSize: '13px', fontWeight: '700', transition: 'all 0.15s',
                background: (i === 0) === isLoginTab ? 'var(--neon)' : 'transparent',
                color: (i === 0) === isLoginTab ? '#0a0f0c' : 'var(--text-secondary)',
              }}
            >{label}</button>
          ))}
        </div>

        {/* Error banner */}
        {errorMsg && (
          <div style={{
            display: 'flex', alignItems: 'center', gap: '10px',
            background: 'var(--danger-dim)', border: '1px solid rgba(239,83,80,0.25)',
            borderRadius: '8px', padding: '12px 14px', color: 'var(--danger)', fontSize: '13px',
          }}>
            <span>⚠</span> <span>{errorMsg}</span>
          </div>
        )}

        {/* Form */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {isLoginTab ? (
            <>
              <div className="form-group">
                <label className="form-label">Nombre de Miembro</label>
                <input className="form-control" value={loginUser} onChange={e => { setLoginUser(e.target.value); setErrorMsg(null); }} placeholder="ej. Milton" />
              </div>
              <div className="form-group">
                <label className="form-label">Código del Hogar</label>
                <input className="form-control" value={loginCode} onChange={e => { setLoginCode(e.target.value); setErrorMsg(null); }} placeholder="HOGAR-XXXX" />
              </div>
              <button className="btn btn-primary" style={{ height: '44px', marginTop: '4px' }} onClick={handleLogin} disabled={isLoading}>
                {isLoading ? 'Entrando...' : 'Ingresar al Hogar'}
              </button>

              <div style={{ display: 'flex', alignItems: 'center', gap: '12px', color: 'var(--text-muted)', fontSize: '11px', fontWeight: '700' }}>
                <div style={{ flex: 1, height: '1px', background: 'var(--border)' }} />
                o accede con cuentas demo
                <div style={{ flex: 1, height: '1px', background: 'var(--border)' }} />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                {[['M', 'Milton', '#0a0f0c', 'var(--neon)'], ['A', 'Alejandra', '#0a0f0c', '#7c83fd']].map(([initial, name, textColor, bg]) => (
                  <button
                    key={name}
                    className="btn"
                    style={{ background: bg, color: textColor, height: '44px', fontWeight: '700' }}
                    onClick={() => { setLoginUser(name); setLoginCode('HOGAR-5892'); onLogin(name, 'HOGAR-5892', false); }}
                  >
                    <span style={{ width: '24px', height: '24px', borderRadius: '50%', background: 'rgba(0,0,0,0.15)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '12px', fontWeight: '800' }}>{initial}</span>
                    {name} (Demo)
                  </button>
                ))}
              </div>
            </>
          ) : (
            <>
              <div className="form-group">
                <label className="form-label">Tu Nombre</label>
                <input className="form-control" value={regUser} onChange={e => { setRegUser(e.target.value); setErrorMsg(null); }} placeholder="ej. Milton" />
              </div>
              <div className="form-group">
                <label className="form-label">Código del Hogar (generado)</label>
                <input className="form-control" value={regCode} onChange={e => { setRegCode(e.target.value); setErrorMsg(null); }} />
              </div>
              <div className="form-group">
                <label className="form-label">Otros Miembros (separados por coma)</label>
                <input className="form-control" value={regMembers} onChange={e => setRegMembers(e.target.value)} placeholder="ej. Pilar, Mateo" />
              </div>
              <button className="btn btn-primary" style={{ height: '44px', marginTop: '4px' }} onClick={handleRegister} disabled={isLoading}>
                {isLoading ? 'Creando Hogar...' : 'Crear y Guardar Hogar'}
              </button>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

// ── Main App ──────────────────────────────────────────────────────────────────
export default function Home() {
  // ── Auth state (persists to localStorage) ──
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [currentUser, setCurrentUser] = useState('');
  const [householdCode, setHouseholdCode] = useState('');

  useEffect(() => {
    const saved = localStorage.getItem('hogar_sincro_session');
    if (saved) {
      try {
        const { user, code } = JSON.parse(saved);
        setCurrentUser(user);
        setHouseholdCode(code);
        setIsLoggedIn(true);
      } catch (_) {}
    }
  }, []);

  const handleLogin = (user, code) => {
    localStorage.setItem('hogar_sincro_session', JSON.stringify({ user, code }));
    setCurrentUser(user);
    setHouseholdCode(code);
    setIsLoggedIn(true);
  };

  const handleLogout = () => {
    localStorage.removeItem('hogar_sincro_session');
    setIsLoggedIn(false);
    setCurrentUser('');
    setHouseholdCode('');
  };

  if (!isLoggedIn) {
    return <LoginScreen onLogin={handleLogin} />;
  }

  return <Dashboard currentUser={currentUser} householdCode={householdCode} onLogout={handleLogout} />;
}

// ── Dashboard (protected) ──────────────────────────────────────────────────────
function Dashboard({ currentUser, householdCode, onLogout }) {
  const [activeTab, setActiveTab] = useState('dashboard');
  const [geminiModel, setGeminiModel] = useState(() => {
    if (typeof window !== 'undefined') return localStorage.getItem('hogar_sincro_gemini_model') || 'gemini-2.5-flash';
    return 'gemini-2.5-flash';
  });

  // Data States
  const [expenses, setExpenses] = useState([]);
  const [inventory, setInventory] = useState([]);
  const [shopping, setShopping] = useState([]);
  const [notifications, setNotifications] = useState([]);
  const [showNotificationDrawer, setShowNotificationDrawer] = useState(false);
  const [neonStatus, setNeonStatus] = useState('CONNECTING');
  const [loading, setLoading] = useState(true);
  const { toasts, addToast, removeToast } = useToasts();

  // Confirm modal state (replaces browser confirm())
  const [confirmModal, setConfirmModal] = useState(null); // { message, onConfirm }

  // Forms States
  const [expenseForm, setExpenseForm] = useState({ title: '', amount: '', category: 'Alimentos', isRecurring: false, recurringDueDate: '', paidBy: currentUser || 'Milton' });
  const [shoppingForm, setShoppingForm] = useState({ productName: '', quantityToBuy: '1', unit: 'u', estimatedPrice: '', targetStore: '' });
  const [inventoryForm, setInventoryForm] = useState({ name: '', currentStock: '', unit: 'u', bestStore: '', bestPrice: '' });

  // Scanner States
  const [isScanning, setIsScanning] = useState(false);
  const [scanResult, setScanResult] = useState(null);
  const [funnyMessage, setFunnyMessage] = useState('');
  const fileInputRef = useRef(null);

  const funnyMessages = [
    "🤖 Sobornando a la inteligencia artificial con galletas virtuales...",
    "🔍 Analizando el ticket... ¿De verdad compraste tanto chocolate?",
    "🧠 Traduciendo la letra horrible del cajero a lenguaje binario...",
    "💸 Contabilizando la tragedia financiera de esta semana...",
    "🛒 Negociando con la base de datos para que no te juzgue por tus compras...",
    "🦖 Buscando fósiles de dinosaurios en tu cuenta bancaria...",
    "⚡ Gemini está regañando al servidor por la lentitud de tu conexión...",
    "🥛 Confirmando si la leche de almendras califica como alimento o estilo de vida...",
    "🛸 Enviando la lista al espacio exterior por si los marcianos quieren compartir...",
    "🧾 Descifrando jeroglíficos modernos en la sección de totales...",
  ];

  // Pantry Scanner States
  const [isPantryScanning, setIsPantryScanning] = useState(false);
  const [stagedInventory, setStagedInventory] = useState([]);
  const [pantryFunnyMessage, setPantryFunnyMessage] = useState('');
  const pantryFileInputRef = useRef(null);

  const pantryFunnyMessages = [
    "🔍 Analizando estantes... Buscando latas escondidas en el fondo...",
    "🦖 Contando botes de conservas... ¡Cuidado con el polvo de 2021!",
    "🥫 Identificando marcas... ¿Este atún es gourmet o genérico?",
    "🧠 Gemini está regañando al refrigerador por no tener luz propia...",
    "📦 Auditando la despensa... ¿De verdad compraste 5 bolsas de garbanzos?",
    "🛸 Escaneando... Enviando informe de existencias a la nave nodriza...",
  ];

  useEffect(() => {
    let intervalId;
    if (isScanning) {
      const pickRandom = () => setFunnyMessage(funnyMessages[Math.floor(Math.random() * funnyMessages.length)]);
      pickRandom();
      intervalId = setInterval(pickRandom, 2200);
    } else { setFunnyMessage(''); }
    return () => { if (intervalId) clearInterval(intervalId); };
  }, [isScanning]);

  useEffect(() => {
    let intervalId;
    if (isPantryScanning) {
      const pickRandom = () => setPantryFunnyMessage(pantryFunnyMessages[Math.floor(Math.random() * pantryFunnyMessages.length)]);
      pickRandom();
      intervalId = setInterval(pickRandom, 2200);
    } else { setPantryFunnyMessage(''); }
    return () => { if (intervalId) clearInterval(intervalId); };
  }, [isPantryScanning]);

  useEffect(() => { fetchData(); }, []);

  const fetchData = async () => {
    setLoading(true);
    try {
      setNeonStatus('CONNECTING');
      const [resExpenses, resInventory, resShopping] = await Promise.all([
        fetch('/api/expenses'),
        fetch('/api/inventory'),
        fetch('/api/shopping')
      ]);
      if (!resExpenses.ok || !resInventory.ok || !resShopping.ok) throw new Error('Some API routes failed');
      const expensesData = await resExpenses.json();
      const inventoryData = await resInventory.json();
      const shoppingData = await resShopping.json();
      setExpenses(expensesData);
      setInventory(inventoryData);
      setShopping(shoppingData);
      setNeonStatus('CONNECTED');
      generateAlerts(expensesData, inventoryData);
    } catch (err) {
      console.error(err);
      setNeonStatus('ERROR');
      addToast('Error al conectar con Neon Database', 'error');
    } finally {
      setLoading(false);
    }
  };

  const generateAlerts = (expensesList, inventoryList) => {
    const alerts = [];
    inventoryList.forEach(item => {
      const stock = Number(item.current_stock);
      if (stock === 0) {
        alerts.push({ id: `low_${item.id}`, title: `Sin Stock: ${item.name}`, message: `${item.name} está agotado.`, type: 'ALERTA' });
      }
    });
    expensesList.forEach(bill => {
      if (bill.is_recurring && bill.recurring_due_date) {
        alerts.push({ id: `bill_${bill.id}`, title: `Vence Pronto: ${bill.title}`, message: `Vence el ${bill.recurring_due_date}. Costo: $${Number(bill.amount).toFixed(2)}.`, type: 'SERVICIO' });
      }
    });
    setNotifications(alerts);
  };

  // ── App-level confirm modal (replaces browser confirm()) ───────────────────
  const showConfirm = (message) => new Promise(resolve => {
    setConfirmModal({ message, onConfirm: () => { setConfirmModal(null); resolve(true); }, onCancel: () => { setConfirmModal(null); resolve(false); } });
  });

  // ── Mutators ───────────────────────────────────────────────────────────────
  const handleAddExpense = async (e) => {
    e.preventDefault();
    if (!expenseForm.title || !expenseForm.amount) return;
    try {
      const res = await fetch('/api/expenses', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ title: expenseForm.title, amount: parseFloat(expenseForm.amount), category: expenseForm.category, isRecurring: expenseForm.isRecurring, recurringDueDate: expenseForm.isRecurring ? expenseForm.recurringDueDate : null, paidBy: expenseForm.paidBy })
      });
      if (res.ok) {
        setExpenseForm({ title: '', amount: '', category: 'Alimentos', isRecurring: false, recurringDueDate: '', paidBy: currentUser || 'Milton' });
        addToast('Gasto agregado correctamente', 'success');
        fetchData();
      } else {
        const err = await res.json();
        addToast(`Error: ${err.error || 'No se pudo agregar el gasto'}`, 'error');
      }
    } catch (err) {
      addToast('Error de conexión al agregar el gasto', 'error');
    }
  };

  const handleDeleteExpense = async (id) => {
    const confirmed = await showConfirm('¿Eliminar este gasto?');
    if (!confirmed) return;
    try {
      const res = await fetch(`/api/expenses?id=${id}`, { method: 'DELETE' });
      if (res.ok) { addToast('Gasto eliminado', 'warning'); fetchData(); }
      else addToast('Error al eliminar el gasto', 'error');
    } catch (err) { addToast('Error de conexión', 'error'); }
  };

  const handleAddShoppingItem = async (e) => {
    e.preventDefault();
    if (!shoppingForm.productName) return;
    try {
      const res = await fetch('/api/shopping', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ productName: shoppingForm.productName, quantityToBuy: parseFloat(shoppingForm.quantityToBuy), unit: shoppingForm.unit, estimatedPrice: parseFloat(shoppingForm.estimatedPrice || '0'), isBought: false, targetStore: shoppingForm.targetStore || null })
      });
      if (res.ok) {
        setShoppingForm({ productName: '', quantityToBuy: '1', unit: 'u', estimatedPrice: '', targetStore: '' });
        addToast('Artículo agregado a la lista', 'success');
        fetchData();
      } else addToast('Error al agregar artículo', 'error');
    } catch (err) { addToast('Error de conexión', 'error'); }
  };

  const handleToggleBought = async (item) => {
    try {
      const res = await fetch('/api/shopping', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ id: item.id, productName: item.product_name, quantityToBuy: item.quantity_to_buy, unit: item.unit, estimatedPrice: item.estimated_price, isBought: !item.is_bought, targetStore: item.target_store })
      });
      if (res.ok) {
        addToast(item.is_bought ? 'Marcado como pendiente' : '¡Marcado como comprado! Despensa actualizada.', item.is_bought ? 'info' : 'success');
        fetchData();
      }
    } catch (err) { addToast('Error de conexión', 'error'); }
  };

  const handleDeleteShoppingItem = async (id) => {
    const confirmed = await showConfirm('¿Eliminar este artículo de la lista?');
    if (!confirmed) return;
    try {
      const res = await fetch(`/api/shopping?id=${id}`, { method: 'DELETE' });
      if (res.ok) { addToast('Artículo removido', 'warning'); fetchData(); }
      else addToast('Error al eliminar artículo', 'error');
    } catch (err) { addToast('Error de conexión', 'error'); }
  };

  const handleClearBought = async () => {
    const confirmed = await showConfirm('¿Limpiar todos los artículos comprados?');
    if (!confirmed) return;
    try {
      const res = await fetch('/api/shopping?clearBought=true', { method: 'DELETE' });
      if (res.ok) { addToast('Lista de compras depurada', 'info'); fetchData(); }
    } catch (err) { addToast('Error de conexión', 'error'); }
  };

  const handlePantryScan = async (base64Images) => {
    setIsPantryScanning(true);
    try {
      const res = await fetch('/api/scan-inventory', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ images: base64Images || [], model: geminiModel })
      });
      if (res.ok) {
        const result = await res.json();
        const stagedItems = (result.products || []).map(item => ({ ...item, checked: true }));
        setStagedInventory(stagedItems);
        addToast(`Se detectaron ${stagedItems.length} artículos. Revisa la bandeja de validación.`, 'success');
      } else {
        const err = await res.json();
        addToast(`Error al analizar fotos: ${err.error || 'Error desconocido'}`, 'error');
      }
    } catch (err) {
      addToast('Error de conexión con el analizador AI', 'error');
    } finally {
      setIsPantryScanning(false);
    }
  };

  const handleCustomPantryUpload = (e) => {
    const files = Array.from(e.target.files);
    if (files.length === 0) return;
    const limitFiles = files.slice(0, 3);
    const loadedImages = [];
    let loadedCount = 0;
    limitFiles.forEach((file) => {
      const reader = new FileReader();
      reader.onloadend = () => {
        loadedImages.push(reader.result);
        loadedCount++;
        if (loadedCount === limitFiles.length) handlePantryScan(loadedImages);
      };
      reader.readAsDataURL(file);
    });
  };

  const handleUpdateStagedItem = (tempId, field, value) => {
    setStagedInventory(prev => prev.map(item => item.tempId === tempId ? { ...item, [field]: value } : item));
  };

  const handleDeleteStagedItem = (tempId) => {
    setStagedInventory(prev => prev.filter(item => item.tempId !== tempId));
    addToast('Artículo descartado', 'warning');
  };

  const handleToggleStagedChecked = (tempId) => {
    setStagedInventory(prev => prev.map(item => item.tempId === tempId ? { ...item, checked: !item.checked } : item));
  };

  const handleSelectAllStaged = (isChecked) => {
    setStagedInventory(prev => prev.map(item => ({ ...item, checked: isChecked })));
  };

  const handleCommitStaged = async () => {
    const itemsToCommit = stagedInventory.filter(item => item.checked);
    if (itemsToCommit.length === 0) { addToast('No hay artículos seleccionados para validar', 'warning'); return; }
    try {
      setLoading(true);
      const savePromises = itemsToCommit.map(async (item) => {
        const existingRealItem = inventory.find(inv => inv.name.toLowerCase() === item.name.toLowerCase());
        let newStock = Number(item.quantity);
        let unit = item.unit || 'u', bestStore = 'Carga Visual', bestPrice = null;
        if (existingRealItem) {
          newStock = Number(existingRealItem.current_stock) + Number(item.quantity);
          unit = existingRealItem.unit;
          bestStore = existingRealItem.best_store;
          bestPrice = existingRealItem.best_price;
        }
        return fetch('/api/inventory', {
          method: 'POST', headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ id: existingRealItem ? existingRealItem.id : null, name: item.name, currentStock: newStock, minStockAlert: 0, unit, depletionRatePerDay: 0, bestStore, bestPrice })
        });
      });
      await Promise.all(savePromises);
      setStagedInventory(prev => prev.filter(item => !item.checked));
      addToast(`¡${itemsToCommit.length} artículos validados e ingresados a la despensa!`, 'success');
      fetchData();
    } catch (err) {
      addToast('Error al validar provisiones en Neon', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleAddInventory = async (e) => {
    e.preventDefault();
    if (!inventoryForm.name) return;
    try {
      const res = await fetch('/api/inventory', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: inventoryForm.name, currentStock: parseFloat(inventoryForm.currentStock || '0'), minStockAlert: 0, unit: inventoryForm.unit, depletionRatePerDay: 0, bestStore: inventoryForm.bestStore || null, bestPrice: inventoryForm.bestPrice ? parseFloat(inventoryForm.bestPrice) : null })
      });
      if (res.ok) {
        setInventoryForm({ name: '', currentStock: '', unit: 'u', bestStore: '', bestPrice: '' });
        addToast('Provisiones añadidas al Inventario', 'success');
        fetchData();
      } else addToast('Error al agregar al inventario', 'error');
    } catch (err) { addToast('Error de conexión', 'error'); }
  };

  const handleUpdateStock = async (item, delta) => {
    const newStock = Math.max(0, Number(item.current_stock) + delta);
    try {
      const res = await fetch('/api/inventory', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ id: item.id, name: item.name, currentStock: newStock, minStockAlert: item.min_stock_alert, unit: item.unit, depletionRatePerDay: item.depletion_rate_per_day, bestStore: item.best_store, bestPrice: item.best_price, secondBestStore: item.second_best_store, secondBestPrice: item.second_best_price })
      });
      if (res.ok) { addToast(`${item.name}: ${newStock} ${item.unit}`, 'info'); fetchData(); }
    } catch (err) { addToast('Error de conexión', 'error'); }
  };

  const handleDeleteInventory = async (id, name) => {
    const confirmed = await showConfirm(`¿Eliminar "${name}" del inventario?`);
    if (!confirmed) return;
    try {
      const res = await fetch(`/api/inventory?id=${id}`, { method: 'DELETE' });
      if (res.ok) { addToast('Artículo del inventario removido', 'warning'); fetchData(); }
      else addToast('Error al eliminar del inventario', 'error');
    } catch (err) { addToast('Error de conexión', 'error'); }
  };

  const handleScanReceipt = async (base64Image) => {
    setIsScanning(true);
    setScanResult(null);
    try {
      const res = await fetch('/api/scan', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ image: base64Image || '', model: geminiModel })
      });
      if (res.ok) {
        const result = await res.json();
        setScanResult(result);
        addToast('Escaneo completado. ¡Gastos y despensa actualizados!', 'success');
        fetchData();
      } else {
        const err = await res.json();
        addToast(`Error en escaneo: ${err.error || 'Error desconocido'}`, 'error');
      }
    } catch (err) {
      addToast('Error en el servicio de escaneo AI', 'error');
    } finally {
      setIsScanning(false);
    }
  };

  const handleCustomImageUpload = (e) => {
    const file = e.target.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onloadend = () => { handleScanReceipt(reader.result); };
    reader.readAsDataURL(file);
  };

  // Dashboard Stats
  const totalVariableThisMonth = expenses.filter(exp => !exp.is_recurring).reduce((sum, exp) => sum + Number(exp.amount), 0);
  const totalRecurring = expenses.filter(exp => exp.is_recurring).reduce((sum, exp) => sum + Number(exp.amount), 0);
  const lowStockCount = inventory.filter(item => Number(item.current_stock) === 0).length;
  const userInitial = currentUser ? currentUser[0].toUpperCase() : 'U';

  return (
    <div className="app-shell">
      {/* ── App-level Confirm Modal (replaces browser confirm()) ── */}
      {confirmModal && (
        <div style={{
          position: 'fixed', inset: 0, zIndex: 10000,
          background: 'rgba(0,0,0,0.6)', display: 'flex', alignItems: 'center', justifyContent: 'center',
          backdropFilter: 'blur(4px)',
        }}>
          <div style={{
            background: 'var(--bg-elevated)', border: '1px solid var(--border-strong)',
            borderRadius: '16px', padding: '28px', maxWidth: '380px', width: '90%',
            boxShadow: 'var(--shadow-lg)',
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
              <div style={{ width: '36px', height: '36px', borderRadius: '50%', background: 'var(--danger-dim)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <svg width="18" height="18" fill="none" stroke="var(--danger)" strokeWidth="2" viewBox="0 0 24 24"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
              </div>
              <h3 style={{ fontSize: '15px', fontWeight: '700', color: 'var(--text-primary)', margin: 0 }}>Confirmar acción</h3>
            </div>
            <p style={{ fontSize: '13px', color: 'var(--text-secondary)', lineHeight: '1.5', marginBottom: '20px' }}>{confirmModal.message}</p>
            <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end' }}>
              <button className="btn btn-ghost" onClick={confirmModal.onCancel}>Cancelar</button>
              <button className="btn btn-danger" style={{ background: 'var(--danger)', color: 'white' }} onClick={confirmModal.onConfirm}>Confirmar</button>
            </div>
          </div>
        </div>
      )}

      {/* ── Sidebar ── */}
      <aside className="sidebar">
        <div className="sidebar-logo">
          <h1>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" style={{ color: 'var(--neon)' }}>
              <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
              <polyline points="9 22 9 12 15 12 15 22"/>
            </svg>
            Hogar Sincro
          </h1>
          <p>Consola Web Inteligente</p>
        </div>

        <div className="sidebar-section-label">Módulos del Sistema</div>
        <nav className="sidebar-nav">
          {[
            { id: 'dashboard', label: 'Dashboard', icon: <rect x="3" y="3" width="7" height="9" rx="1"/>, icon2: <><rect x="3" y="3" width="7" height="9" rx="1"/><rect x="14" y="3" width="7" height="5" rx="1"/><rect x="14" y="12" width="7" height="9" rx="1"/><rect x="3" y="16" width="7" height="5" rx="1"/></> },
            { id: 'expenses', label: 'Gastos Compartidos', icon2: <><line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></> },
            { id: 'shopping', label: 'Lista de Compras', icon2: <><circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/></> },
            { id: 'inventory', label: 'Despensa y Stock', icon2: <><polyline points="22 12 16 12 14 15 10 15 8 12 2 12"/><path d="M5.45 5.11L2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z"/></> },
            { id: 'scanner', label: 'Escáner AI Recibos', icon2: <><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/></> },
            { id: 'sync', label: 'Sincronización', icon2: <><path d="M21.5 2v6h-6"/><path d="M2.5 22v-6h6"/><path d="M22 11.5A10 10 0 0 0 3.2 7.2M2 12.5a10 10 0 0 0 18.8 4.2"/></> },
          ].map(tab => (
            <button key={tab.id} onClick={() => setActiveTab(tab.id)} className={`nav-item ${activeTab === tab.id ? 'active' : ''}`}>
              <svg width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2">{tab.icon2}</svg>
              {tab.label}
            </button>
          ))}
        </nav>

        <div className="sidebar-footer">
          <div className="user-card">
            <div className="avatar" style={{ background: 'var(--neon)', color: '#0f0f12' }}>{userInitial}</div>
            <div style={{ flex: 1 }}>
              <p style={{ fontSize: '12px', fontWeight: '700', color: 'var(--text-primary)' }}>{currentUser}</p>
              <p style={{ fontSize: '10px', color: 'var(--text-muted)' }}>{householdCode}</p>
            </div>
            <button
              onClick={onLogout}
              title="Cerrar sesión"
              className="btn btn-ghost btn-icon"
              style={{ width: '28px', height: '28px', padding: '6px', flexShrink: 0 }}
            >
              <svg width="13" height="13" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/>
              </svg>
            </button>
          </div>
        </div>
      </aside>

      {/* ── Bottom Nav (mobile only, hidden on desktop via CSS) ── */}
      <nav className="bottom-nav">
        {[
          { id: 'dashboard', label: 'Inicio',
            icon: <><rect x="3" y="3" width="7" height="9" rx="1"/><rect x="14" y="3" width="7" height="5" rx="1"/><rect x="14" y="12" width="7" height="9" rx="1"/><rect x="3" y="16" width="7" height="5" rx="1"/></> },
          { id: 'expenses', label: 'Gastos',
            icon: <><line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></> },
          { id: 'shopping', label: 'Compras',
            icon: <><circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/></> },
          { id: 'inventory', label: 'Despensa',
            icon: <><polyline points="22 12 16 12 14 15 10 15 8 12 2 12"/><path d="M5.45 5.11L2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z"/></> },
          { id: 'scanner', label: 'Scanner',
            icon: <><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/></> },
          { id: 'sync', label: 'Sincro',
            icon: <><path d="M21.5 2v6h-6"/><path d="M2.5 22v-6h6"/><path d="M22 11.5A10 10 0 0 0 3.2 7.2M2 12.5a10 10 0 0 0 18.8 4.2"/></> },
        ].map(tab => (
          <button
            key={tab.id}
            className={`bottom-nav-item ${activeTab === tab.id ? 'active' : ''}`}
            onClick={() => setActiveTab(tab.id)}
          >
            <svg width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
              {tab.icon}
            </svg>
            {tab.label}
          </button>
        ))}
      </nav>

      {/* ── Main content ── */}
      <main className="main-content">
        <header className="topbar">
          <div className="topbar-title">
            {/* Mobile: show app icon + tab name; Desktop: show full titles */}
            <h2 style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--neon)" strokeWidth="2.5" style={{ flexShrink: 0 }}>
                <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
                <polyline points="9 22 9 12 15 12 15 22"/>
              </svg>
              {activeTab === 'dashboard' && 'Dashboard'}
              {activeTab === 'expenses' && 'Gastos'}
              {activeTab === 'shopping' && 'Compras'}
              {activeTab === 'inventory' && 'Despensa'}
              {activeTab === 'scanner' && 'Scanner AI'}
              {activeTab === 'sync' && 'Sincronización'}
            </h2>
            <p>{householdCode} · {currentUser}</p>
          </div>

          <div className="topbar-actions">
            {/* Neon status — only show on larger screens */}
            <div className="btn btn-ghost btn-sm neon-status-badge" style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'default' }}>
              <span className={`status-dot ${neonStatus === 'CONNECTED' ? 'connected' : neonStatus === 'CONNECTING' ? 'connecting' : 'error'}`}></span>
              <span style={{ fontSize: '11px', fontWeight: '700', color: 'var(--text-secondary)' }}>
                {neonStatus === 'CONNECTED' ? 'ONLINE' : neonStatus === 'CONNECTING' ? '...' : 'OFFLINE'}
              </span>
            </div>

            <div style={{ position: 'relative' }}>
              <button onClick={() => setShowNotificationDrawer(!showNotificationDrawer)} className="btn btn-ghost btn-icon" style={{ position: 'relative' }}>
                <svg width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
                  <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
                </svg>
                {notifications.length > 0 && (
                  <span style={{ position: 'absolute', top: '2px', right: '2px', backgroundColor: 'var(--danger)', color: 'white', borderRadius: '50%', fontSize: '9px', width: '16px', height: '16px', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 'bold' }}>
                    {notifications.length}
                  </span>
                )}
              </button>

              {showNotificationDrawer && (
                <div className="notif-drawer">
                  <div className="notif-header">
                    <span>Alertas Recientes</span>
                    <button onClick={() => setShowNotificationDrawer(false)} className="btn btn-ghost btn-sm" style={{ padding: '2px 6px' }}>✕</button>
                  </div>
                  <div className="notif-list">
                    {notifications.length === 0 ? (
                      <p style={{ color: 'var(--text-muted)', fontSize: '11.5px', textAlign: 'center', padding: '12px' }}>Sin alertas activas.</p>
                    ) : (
                      notifications.map((notif) => (
                        <div key={notif.id} className="notif-item" style={{ borderLeftColor: notif.type === 'ALERTA' ? 'var(--danger)' : notif.type === 'ADVERTENCIA' ? 'var(--warning)' : 'var(--info)' }}>
                          <p style={{ fontSize: '12px', fontWeight: '700', color: 'var(--text-primary)' }}>{notif.title}</p>
                          <p style={{ fontSize: '11px', color: 'var(--text-secondary)', marginTop: '2px' }}>{notif.message}</p>
                        </div>
                      ))
                    )}
                  </div>
                </div>
              )}
            </div>

            <button onClick={fetchData} className="btn btn-ghost" disabled={loading}>
              <svg width="13" height="13" fill="none" stroke="currentColor" strokeWidth="2" style={{ marginRight: '4px' }}><path d="M21.5 2v6h-6M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.67"/></svg>
              Actualizar
            </button>
          </div>
        </header>

        <div className="page-body">
          {loading && expenses.length === 0 ? (
            <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', flexDirection: 'column', gap: '16px', minHeight: '300px' }}>
              <div className="spinner"></div>
              <p style={{ color: 'var(--text-secondary)', fontSize: '13px' }}>Conectando a base de datos de Neon PostgreSQL...</p>
            </div>
          ) : (
            <>
              {/* ── TAB: DASHBOARD ── */}
              {activeTab === 'dashboard' && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
                  <div className="stats-grid">
                    <div className="stat-card">
                      <div className="stat-icon" style={{ background: 'rgba(0, 230, 118, 0.12)', color: 'var(--neon)' }}>💵</div>
                      <span className="stat-label">Gastos Variables del Mes</span>
                      <span className="stat-value">${totalVariableThisMonth.toFixed(2)}</span>
                      <span className="stat-sub">Registros cotidianos</span>
                    </div>
                    <div className="stat-card">
                      <div className="stat-icon" style={{ background: 'rgba(124, 131, 253, 0.15)', color: 'var(--info)' }}>📅</div>
                      <span className="stat-label">Costo Mensual Fijo Recurrente</span>
                      <span className="stat-value" style={{ color: 'var(--info)' }}>${totalRecurring.toFixed(2)}</span>
                      <span className="stat-sub">Servicios e impuestos</span>
                    </div>
                    <div className="stat-card" style={{ borderColor: lowStockCount > 0 ? 'rgba(239, 83, 80, 0.2)' : 'var(--border)' }}>
                      <div className="stat-icon" style={{ background: lowStockCount > 0 ? 'var(--danger-dim)' : 'rgba(102, 187, 106, 0.15)', color: lowStockCount > 0 ? 'var(--danger)' : 'var(--success)' }}>📦</div>
                      <span className="stat-label">Productos con Stock Bajo</span>
                      <span className="stat-value" style={{ color: lowStockCount > 0 ? 'var(--danger)' : 'var(--success)' }}>{lowStockCount}</span>
                      <span className="stat-sub">Requieren compra urgente</span>
                    </div>
                  </div>

                  <div className="panel-split" style={{ gridTemplateColumns: '1.2fr 1fr' }}>
                    <div className="card">
                      <div className="card-header">
                        <span className="card-title">Gastos Recientes</span>
                        <button onClick={() => setActiveTab('expenses')} className="btn btn-ghost btn-sm">Ver todos</button>
                      </div>
                      <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                        {expenses.slice(0, 5).map(exp => (
                          <div key={exp.id} className="list-row">
                            <div style={{ flex: 1, display: 'flex', alignItems: 'center', gap: '12px' }}>
                              <span style={{ fontSize: '18px' }}>{exp.is_recurring ? '📅' : '🛍️'}</span>
                              <div>
                                <p style={{ fontSize: '13px', fontWeight: '600', color: 'var(--text-primary)' }}>{exp.title}</p>
                                <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>{exp.category} • por {exp.paid_by}</span>
                              </div>
                            </div>
                            <div style={{ textAlign: 'right' }}>
                              <span style={{ fontWeight: '700', fontSize: '13px', color: 'var(--text-primary)' }}>${Number(exp.amount).toFixed(2)}</span>
                              {exp.is_recurring && <div style={{ fontSize: '9px', color: 'var(--warning)', fontWeight: 'bold' }}>RECURRENTE</div>}
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>

                    <div className="card">
                      <div className="card-header">
                        <span className="card-title">Alertas de Despensa</span>
                        <button onClick={() => setActiveTab('inventory')} className="btn btn-ghost btn-sm">Gestionar</button>
                      </div>
                      <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                        {inventory.slice(0, 5).map(item => {
                          const isDepleted = Number(item.current_stock) === 0;
                          return (
                            <div key={item.id} className="list-row" style={{ borderColor: isDepleted ? 'rgba(239, 83, 80, 0.2)' : 'transparent' }}>
                              <div style={{ flex: 1, display: 'flex', alignItems: 'center', gap: '8px' }}>
                                <span className="status-dot" style={{ backgroundColor: isDepleted ? 'var(--danger)' : 'var(--success)' }}></span>
                                <span style={{ fontSize: '13px', fontWeight: '600', color: 'var(--text-primary)' }}>{item.name}</span>
                              </div>
                              <span style={{ fontSize: '12px', fontWeight: '600', color: isDepleted ? 'var(--danger)' : 'var(--text-secondary)' }}>
                                {isDepleted ? 'AGOTADO' : `${item.current_stock} ${item.unit}`}
                              </span>
                            </div>
                          );
                        })}
                      </div>
                    </div>
                  </div>
                </div>
              )}

              {/* ── TAB: EXPENSES ── */}
              {activeTab === 'expenses' && (
                <div className="panel-split">
                  <div className="card card-neon">
                    <div className="card-header">
                      <span className="card-title" style={{ color: 'var(--neon)' }}>Añadir Gasto</span>
                    </div>
                    <form onSubmit={handleAddExpense} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                      <div className="form-group">
                        <label className="form-label">Concepto</label>
                        <input type="text" className="form-control" value={expenseForm.title} onChange={e => setExpenseForm({...expenseForm, title: e.target.value})} placeholder="ej. Luz Eléctrica" required />
                      </div>
                      <div className="form-group">
                        <label className="form-label">Importe ($)</label>
                        <input type="number" step="0.01" className="form-control" value={expenseForm.amount} onChange={e => setExpenseForm({...expenseForm, amount: e.target.value})} placeholder="0.00" required />
                      </div>
                      <div className="form-group">
                        <label className="form-label">Categoría</label>
                        <select className="form-control" value={expenseForm.category} onChange={e => setExpenseForm({...expenseForm, category: e.target.value})}>
                          <option value="Alquiler">Alquiler</option>
                          <option value="Servicio">Servicio</option>
                          <option value="Alimentos">Alimentos</option>
                          <option value="Diverso">Diverso</option>
                        </select>
                      </div>
                      <div className="form-group">
                        <label className="form-label">Quién Pagó</label>
                        <select className="form-control" value={expenseForm.paidBy} onChange={e => setExpenseForm({...expenseForm, paidBy: e.target.value})}>
                          <option value="Milton">Milton</option>
                          <option value="Alejandra">Alejandra</option>
                        </select>
                      </div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px', margin: '6px 0' }}>
                        <input type="checkbox" id="isRecurring" checked={expenseForm.isRecurring} onChange={e => setExpenseForm({...expenseForm, isRecurring: e.target.checked})} style={{ cursor: 'pointer', accentColor: 'var(--neon)' }} />
                        <label htmlFor="isRecurring" style={{ fontSize: '12px', cursor: 'pointer', color: 'var(--text-secondary)' }}>¿Gasto recurrente mensual?</label>
                      </div>
                      {expenseForm.isRecurring && (
                        <div className="form-group">
                          <label className="form-label">Día de cobro</label>
                          <input type="text" className="form-control" value={expenseForm.recurringDueDate} onChange={e => setExpenseForm({...expenseForm, recurringDueDate: e.target.value})} placeholder="ej. Día 10 de cada mes" />
                        </div>
                      )}
                      <button type="submit" className="btn btn-primary" style={{ marginTop: '6px' }}>Añadir Gasto</button>
                    </form>
                  </div>

                  <div className="card">
                    <div className="card-header">
                      <span className="card-title">Listado de Gastos Registrados</span>
                      <span className="badge badge-neon">{expenses.length} Gastos</span>
                    </div>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', maxHeight: '550px', overflowY: 'auto' }}>
                      {expenses.length === 0 ? (
                        <p style={{ color: 'var(--text-muted)', textAlign: 'center', padding: '40px' }}>No hay gastos registrados.</p>
                      ) : (
                        expenses.map(exp => (
                          <div key={exp.id} className="list-row" style={{ borderLeft: `3px solid ${exp.is_recurring ? 'var(--info)' : 'var(--neon)'}` }}>
                            <div style={{ flex: 1 }}>
                              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                <span style={{ fontSize: '13px', fontWeight: '700', color: 'var(--text-primary)' }}>{exp.title}</span>
                                {exp.is_recurring && <span className="badge badge-info">Fijo Mensual</span>}
                              </div>
                              <p style={{ fontSize: '11px', color: 'var(--text-muted)', marginTop: '2px' }}>
                                Categoría: {exp.category} • Pagado por {exp.paid_by} {exp.is_recurring && `• Vence: ${exp.recurring_due_date}`}
                              </p>
                            </div>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                              <span style={{ fontWeight: '700', fontSize: '14px', color: 'var(--text-primary)' }}>${Number(exp.amount).toFixed(2)}</span>
                              <button onClick={() => handleDeleteExpense(exp.id)} className="btn btn-ghost btn-sm btn-danger" style={{ padding: '6px' }}>
                                <svg width="14" height="14" fill="none" stroke="currentColor" strokeWidth="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
                              </button>
                            </div>
                          </div>
                        ))
                      )}
                    </div>
                  </div>
                </div>
              )}

              {/* ── TAB: SHOPPING ── */}
              {activeTab === 'shopping' && (
                <div className="panel-split">
                  <div className="card card-neon">
                    <div className="card-header">
                      <span className="card-title" style={{ color: 'var(--neon)' }}>Planificar Compra</span>
                    </div>
                    <form onSubmit={handleAddShoppingItem} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                      <div className="form-group">
                        <label className="form-label">Nombre del Producto</label>
                        <input type="text" className="form-control" value={shoppingForm.productName} onChange={e => setShoppingForm({...shoppingForm, productName: e.target.value})} placeholder="ej. Queso Cheddar" required />
                      </div>
                      <div className="grid-2">
                        <div className="form-group">
                          <label className="form-label">Cantidad</label>
                          <input type="number" step="0.1" className="form-control" value={shoppingForm.quantityToBuy} onChange={e => setShoppingForm({...shoppingForm, quantityToBuy: e.target.value})} required />
                        </div>
                        <div className="form-group">
                          <label className="form-label">Unidad</label>
                          <select className="form-control" value={shoppingForm.unit} onChange={e => setShoppingForm({...shoppingForm, unit: e.target.value})}>
                            <option value="u">unidades</option>
                            <option value="kg">kg</option>
                            <option value="litros">litros</option>
                            <option value="paquetes">paquetes</option>
                          </select>
                        </div>
                      </div>
                      <div className="form-group">
                        <label className="form-label">Precio Estimado ($)</label>
                        <input type="number" step="0.01" className="form-control" value={shoppingForm.estimatedPrice} onChange={e => setShoppingForm({...shoppingForm, estimatedPrice: e.target.value})} placeholder="0.00 (Opcional)" />
                      </div>
                      <div className="form-group">
                        <label className="form-label">Comercio Objetivo</label>
                        <input type="text" className="form-control" value={shoppingForm.targetStore} onChange={e => setShoppingForm({...shoppingForm, targetStore: e.target.value})} placeholder="ej. Walmart" />
                      </div>
                      <button type="submit" className="btn btn-primary" style={{ marginTop: '6px' }}>Agregar a Lista</button>
                    </form>
                  </div>

                  <div className="card">
                    <div className="card-header">
                      <span className="card-title">Lista de Artículos a Adquirir</span>
                      {shopping.some(item => item.is_bought) && (
                        <button onClick={handleClearBought} className="btn btn-danger btn-sm">Limpiar Comprados</button>
                      )}
                    </div>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', maxHeight: '550px', overflowY: 'auto' }}>
                      {shopping.length === 0 ? (
                        <p style={{ color: 'var(--text-muted)', textAlign: 'center', padding: '40px' }}>La lista de compras está vacía.</p>
                      ) : (() => {
                        const pending = shopping.filter(i => !i.is_bought);
                        const bought = shopping.filter(i => i.is_bought);
                        const renderItem = item => (
                          <div key={item.id} className="list-row" style={{ opacity: item.is_bought ? 0.55 : 1, background: item.is_bought ? 'rgba(0,230,118,0.03)' : 'var(--bg-elevated)' }}>
                            <div style={{ flex: 1, display: 'flex', alignItems: 'center', gap: '14px' }}>
                              <button onClick={() => handleToggleBought(item)} style={{ background: 'none', border: 'none', cursor: 'pointer', color: item.is_bought ? 'var(--neon)' : 'var(--text-muted)', flexShrink: 0 }}>
                                {item.is_bought ? (
                                  <svg width="20" height="20" viewBox="0 0 24 24" fill="var(--neon)" stroke="var(--neon)" strokeWidth="2"><circle cx="12" cy="12" r="10"/><polyline points="9 12 11 14 15 10"/></svg>
                                ) : (
                                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="12" r="10"/></svg>
                                )}
                              </button>
                              <div>
                                <span style={{ fontSize: '13px', fontWeight: '700', textDecoration: item.is_bought ? 'line-through' : 'none', color: item.is_bought ? 'var(--text-muted)' : 'var(--text-primary)' }}>{item.product_name}</span>
                                <p style={{ fontSize: '11px', color: 'var(--text-muted)', marginTop: '2px' }}>
                                  {item.quantity_to_buy} {item.unit}{item.target_store ? ` • ${item.target_store}` : ''}{Number(item.estimated_price) > 0 ? ` • $${Number(item.estimated_price).toFixed(2)}` : ''}
                                </p>
                              </div>
                            </div>
                            <button onClick={() => handleDeleteShoppingItem(item.id)} className="btn btn-ghost btn-sm btn-danger" style={{ padding: '6px' }}>
                              <svg width="13" height="13" fill="none" stroke="currentColor" strokeWidth="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"/></svg>
                            </button>
                          </div>
                        );
                        return (
                          <>
                            {pending.map(renderItem)}
                            {bought.length > 0 && (
                              <>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', padding: '4px 0', marginTop: '4px' }}>
                                  <div style={{ flex: 1, height: '1px', background: 'var(--border)' }} />
                                  <span style={{ fontSize: '10px', fontWeight: '700', color: 'var(--text-muted)', letterSpacing: '1px' }}>COMPRADOS ({bought.length})</span>
                                  <div style={{ flex: 1, height: '1px', background: 'var(--border)' }} />
                                </div>
                                {bought.map(renderItem)}
                              </>
                            )}
                          </>
                        );
                      })()}
                    </div>
                  </div>
                </div>
              )}

              {/* ── TAB: INVENTORY ── */}
              {activeTab === 'inventory' && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
                  {/* Carga rápida por foto */}
                  <div className="card card-neon">
                    <div className="card-header">
                      <span className="card-title" style={{ color: 'var(--neon)' }}>
                        📸 Carga Rápida de Stock por Foto (Gemini AI)
                      </span>
                    </div>
                    <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr', gap: '20px', alignItems: 'start' }}>
                      <div>
                        <p style={{ color: 'var(--text-secondary)', fontSize: '12px', lineHeight: '1.5', marginBottom: '14px' }}>
                          Sube hasta 3 fotos de tu despensa o alacena para escanear y detectar automáticamente todas tus provisiones. Gemini realizará una auditoría rigurosa de doble paso para prevenir distorsiones de inventario.
                        </p>
                      </div>
                      <div>
                        <div className="upload-zone" onClick={() => pantryFileInputRef.current.click()} style={{ padding: '40px 20px' }}>
                          <input type="file" accept="image/*" multiple={true} style={{ display: 'none' }} ref={pantryFileInputRef} onChange={handleCustomPantryUpload} />
                          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ margin: '0 auto 8px auto', color: 'var(--text-secondary)' }}><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
                          <p style={{ fontSize: '12px', fontWeight: '700', color: 'var(--text-primary)' }}>Tomar o Subir Fotos de Estanterías</p>
                          <p style={{ color: 'var(--text-muted)', fontSize: '11px', marginTop: '2px' }}>Soporta hasta 3 fotos a la vez</p>
                        </div>

                        {isPantryScanning && (
                          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '10px', padding: '14px', background: 'var(--neon-dim)', borderRadius: '10px', border: '1px solid var(--neon-border)', marginTop: '12px', textAlign: 'center' }}>
                            <div className="spinner"></div>
                            <p style={{ fontSize: '12.5px', color: 'var(--neon)', fontWeight: '700', minHeight: '36px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                              {pantryFunnyMessage || "Analizando estantes..."}
                            </p>
                          </div>
                        )}
                      </div>
                    </div>
                  </div>

                  {/* Staging area */}
                  {stagedInventory.length > 0 && (
                    <div className="card card-neon" style={{ background: 'rgba(0, 230, 118, 0.02)', borderColor: 'rgba(0, 230, 118, 0.25)' }}>
                      <div className="card-header">
                        <span className="card-title" style={{ color: 'var(--neon)', display: 'flex', alignItems: 'center', gap: '8px' }}>
                          📥 Bandeja de Aprobación de Inventario Detectado
                          <span className="badge badge-neon">{stagedInventory.length} por validar</span>
                        </span>
                        <div style={{ display: 'flex', gap: '8px' }}>
                          <button onClick={handleCommitStaged} className="btn btn-primary btn-sm">✅ Validar Seleccionados</button>
                          <button onClick={async () => { const ok = await showConfirm('¿Descartar todos los artículos de la bandeja?'); if (ok) { setStagedInventory([]); addToast('Bandeja vaciada', 'warning'); }}} className="btn btn-ghost btn-danger btn-sm">🗑️ Descartar Todo</button>
                        </div>
                      </div>
                      <p style={{ color: 'var(--text-secondary)', fontSize: '11.5px', marginBottom: '14px', lineHeight: '1.4' }}>
                        Revisa uno a uno los artículos detectados. Edita nombre, marca, cantidad y unidad directamente, luego marca la casilla para agregarlos al inventario.
                      </p>
                      <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', maxHeight: '350px', overflowY: 'auto', paddingRight: '4px' }}>
                        <div style={{ display: 'grid', gridTemplateColumns: '48px 2fr 1.2fr 1fr 1fr 40px', gap: '10px', padding: '4px 12px', fontSize: '10.5px', fontWeight: '700', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                          <div>Validar</div><div>Nombre del Producto</div><div>Marca / Detalle</div><div>Cantidad</div><div>Unidad</div><div style={{ textAlign: 'center' }}>Descartar</div>
                        </div>
                        {stagedInventory.map(item => (
                          <div key={item.tempId} className="list-row" style={{ display: 'grid', gridTemplateColumns: '48px 2fr 1.2fr 1fr 1fr 40px', gap: '10px', padding: '8px 12px', alignItems: 'center' }}>
                            <div style={{ display: 'flex', justifyContent: 'center' }}>
                              <input type="checkbox" checked={item.checked} onChange={() => handleToggleStagedChecked(item.tempId)} style={{ width: '16px', height: '16px', accentColor: 'var(--neon)', cursor: 'pointer' }} />
                            </div>
                            <div><input type="text" className="form-control" style={{ padding: '6px 10px', fontSize: '12.5px' }} value={item.name} onChange={(e) => handleUpdateStagedItem(item.tempId, 'name', e.target.value)} /></div>
                            <div><input type="text" className="form-control" style={{ padding: '6px 10px', fontSize: '12.5px' }} value={item.brand} onChange={(e) => handleUpdateStagedItem(item.tempId, 'brand', e.target.value)} /></div>
                            <div><input type="number" step="1" min="0" className="form-control" style={{ padding: '6px 10px', fontSize: '12.5px' }} value={item.quantity} onChange={(e) => handleUpdateStagedItem(item.tempId, 'quantity', e.target.value)} /></div>
                            <div><input type="text" className="form-control" style={{ padding: '6px 10px', fontSize: '12.5px' }} value={item.unit} onChange={(e) => handleUpdateStagedItem(item.tempId, 'unit', e.target.value)} /></div>
                            <div style={{ display: 'flex', justifyContent: 'center' }}>
                              <button onClick={() => handleDeleteStagedItem(item.tempId)} className="btn btn-ghost btn-sm btn-danger btn-icon" style={{ width: '28px', height: '28px', padding: '6px' }}>
                                <svg width="12" height="12" fill="none" stroke="currentColor" strokeWidth="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"/></svg>
                              </button>
                            </div>
                          </div>
                        ))}
                      </div>
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '12px', paddingTop: '12px', borderTop: '1px solid var(--border)', fontSize: '12px' }}>
                        <div style={{ display: 'flex', gap: '14px', alignItems: 'center' }}>
                          <button type="button" onClick={() => handleSelectAllStaged(true)} className="btn btn-ghost btn-sm" style={{ fontSize: '11px', padding: '4px 8px' }}>Seleccionar Todos</button>
                          <button type="button" onClick={() => handleSelectAllStaged(false)} className="btn btn-ghost btn-sm" style={{ fontSize: '11px', padding: '4px 8px' }}>Deseleccionar Todos</button>
                        </div>
                        <span style={{ color: 'var(--text-secondary)', fontWeight: '600' }}>Marcados: {stagedInventory.filter(i => i.checked).length} de {stagedInventory.length}</span>
                      </div>
                    </div>
                  )}

                  <div className="panel-split">
                    <div className="card card-neon">
                      <div className="card-header">
                        <span className="card-title" style={{ color: 'var(--neon)' }}>Control de Inventario</span>
                      </div>
                      <form onSubmit={handleAddInventory} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                        <div className="form-group">
                          <label className="form-label">Nombre del Producto</label>
                          <input type="text" className="form-control" value={inventoryForm.name} onChange={e => setInventoryForm({...inventoryForm, name: e.target.value})} placeholder="ej. Arroz" required />
                        </div>
                        <div className="grid-2">
                          <div className="form-group">
                            <label className="form-label">Stock Actual</label>
                            <input type="number" step="1" min="0" className="form-control" value={inventoryForm.currentStock} onChange={e => setInventoryForm({...inventoryForm, currentStock: e.target.value})} placeholder="0" required />
                          </div>
                          <div className="form-group">
                            <label className="form-label">Unidad</label>
                            <input type="text" className="form-control" value={inventoryForm.unit} onChange={e => setInventoryForm({...inventoryForm, unit: e.target.value})} placeholder="ej. kg, l, u" required />
                          </div>
                        </div>
                        <div className="form-group">
                          <label className="form-label">Mejor Comercio Histórico</label>
                          <input type="text" className="form-control" value={inventoryForm.bestStore} onChange={e => setInventoryForm({...inventoryForm, bestStore: e.target.value})} placeholder="ej. Mercadona" />
                        </div>
                        <div className="form-group">
                          <label className="form-label">Mejor Precio Registrado ($)</label>
                          <input type="number" step="0.01" className="form-control" value={inventoryForm.bestPrice} onChange={e => setInventoryForm({...inventoryForm, bestPrice: e.target.value})} placeholder="0.00" />
                        </div>
                        <button type="submit" className="btn btn-primary" style={{ marginTop: '6px' }}>Agregar al Inventario</button>
                      </form>
                    </div>

                    <div className="card">
                      <div className="card-header">
                        <span className="card-title">Artículos en Despensa y Estado</span>
                        <span className="badge badge-neon">{inventory.length} Productos</span>
                      </div>
                      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(230px, 1fr))', gap: '12px', maxHeight: '550px', overflowY: 'auto' }}>
                        {inventory.length === 0 ? (
                          <p style={{ color: 'var(--text-muted)', textAlign: 'center', padding: '40px', gridColumn: '1 / -1' }}>No hay provisiones registradas.</p>
                        ) : (
                          inventory.map(item => {
                            const stock = Number(item.current_stock);
                            const isDepleted = stock === 0;
                            const stockColor = isDepleted ? 'var(--danger)' : 'var(--success)';
                            return (
                              <div key={item.id} className={`inv-card ${isDepleted ? 'low' : ''}`} style={{ borderLeft: `4px solid ${stockColor}` }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                  <h4 style={{ fontSize: '13px', fontWeight: '700', color: 'var(--text-primary)', flex: 1, marginRight: '6px' }}>{item.name}</h4>
                                  {isDepleted && <span style={{ fontSize: '9px', fontWeight: '800', background: 'var(--danger)', color: 'white', padding: '2px 5px', borderRadius: '4px', flexShrink: 0 }}>AGOTADO</span>}
                                  <button onClick={() => handleDeleteInventory(item.id, item.name)} className="btn btn-ghost btn-icon btn-danger btn-sm" style={{ width: '24px', height: '24px', padding: '4px', marginLeft: '4px' }}>
                                    <svg width="11" height="11" fill="none" stroke="currentColor" strokeWidth="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"/></svg>
                                  </button>
                                </div>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginTop: '4px' }}>
                                  <button onClick={() => handleUpdateStock(item, -1)} className="btn btn-ghost btn-sm" style={{ padding: '2px 8px', fontSize: '14px', fontWeight: '700' }}>−</button>
                                  <span style={{ fontSize: '15px', fontWeight: '800', color: stockColor, minWidth: '32px', textAlign: 'center' }}>{stock}</span>
                                  <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>{item.unit}</span>
                                  <button onClick={() => handleUpdateStock(item, 1)} className="btn btn-ghost btn-sm" style={{ padding: '2px 8px', fontSize: '14px', fontWeight: '700' }}>+</button>
                                </div>
                                {item.best_store && (
                                  <div style={{ background: 'rgba(255,255,255,0.03)', borderRadius: '6px', padding: '6px', fontSize: '10.5px', color: 'var(--text-secondary)', marginTop: '4px' }}>
                                    <div>💵 Mín: ${Number(item.best_price).toFixed(2)} ({item.best_store})</div>
                                    {item.second_best_store && <div style={{ marginTop: '2px' }}>🥈 Alt: ${Number(item.second_best_price).toFixed(2)} ({item.second_best_store})</div>}
                                  </div>
                                )}
                              </div>
                            );
                          })
                        )}
                      </div>
                    </div>
                  </div>
                </div>
              )}

              {/* ── TAB: SCANNER ── */}
              {activeTab === 'scanner' && (
                <div className="panel-split" style={{ gridTemplateColumns: '1.1fr 1fr' }}>
                  <div className="card card-neon" style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
                    <div>
                      <h3 style={{ fontSize: '1.1rem', fontWeight: 'bold', marginBottom: '6px' }}>Cargar Recibo / Ticket de Compra</h3>
                      <p style={{ color: 'var(--text-secondary)', fontSize: '12px', lineHeight: '1.5' }}>
                        Mapea y extrae información automáticamente usando Gemini AI. Se insertarán los gastos correspondientes y se actualizarán los precios mínimos históricos y stock de provisiones.
                      </p>
                    </div>

                    <div className="upload-zone" onClick={() => fileInputRef.current.click()}>
                      <input type="file" accept="image/*" style={{ display: 'none' }} ref={fileInputRef} onChange={handleCustomImageUpload} />
                      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ margin: '0 auto 8px auto', color: 'var(--text-secondary)' }}><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
                      <p style={{ fontSize: '12px', fontWeight: '700', color: 'var(--text-primary)' }}>Subir imagen de ticket</p>
                      <p style={{ color: 'var(--text-muted)', fontSize: '11px', marginTop: '2px' }}>Formatos PNG, JPG o JPEG — análisis real con Gemini</p>
                    </div>

                    {isScanning && (
                      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '12px', padding: '18px', background: 'var(--neon-dim)', borderRadius: '10px', border: '1px solid var(--neon-border)', textAlign: 'center' }}>
                        <div className="spinner"></div>
                        <p style={{ fontSize: '12px', color: 'var(--neon)', fontWeight: '700', minHeight: '32px', display: 'flex', alignItems: 'center', justifyContent: 'center', transition: 'all 0.3s ease' }}>
                          {funnyMessage || "Iniciando análisis inteligente..."}
                        </p>
                      </div>
                    )}
                  </div>

                  <div className="card">
                    <div className="card-header">
                      <span className="card-title">Resultado de Extracción</span>
                    </div>
                    {!scanResult ? (
                      <p style={{ color: 'var(--text-muted)', textAlign: 'center', padding: '40px' }}>Sube un ticket para visualizar la información estructurada extraída por Gemini.</p>
                    ) : (
                      <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                        <div style={{ borderBottom: '1px solid var(--border)', paddingBottom: '12px' }}>
                          <span className="badge badge-neon">Importación Exitosa</span>
                          <h4 style={{ fontSize: '15px', fontWeight: '800', color: 'var(--text-primary)', marginTop: '6px' }}>{scanResult.storeName}</h4>
                          <p style={{ color: 'var(--text-muted)', fontSize: '11.5px', marginTop: '2px' }}>Categoría: {scanResult.category}</p>
                        </div>
                        <div>
                          <p style={{ fontSize: '11px', color: 'var(--text-muted)', fontWeight: '700', marginBottom: '6px' }}>ARTÍCULOS REGISTRADOS:</p>
                          <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                            {scanResult.items.map((item, idx) => (
                              <div key={idx} className="list-row" style={{ padding: '8px 12px' }}>
                                <div style={{ flex: 1, fontSize: '12.5px', fontWeight: '600', color: 'var(--text-primary)' }}>
                                  {item.name} <span style={{ color: 'var(--text-muted)', fontSize: '11px' }}>(x{item.quantity})</span>
                                </div>
                                <span style={{ fontWeight: '700', color: 'var(--neon)', fontSize: '12.5px' }}>${Number(item.price).toFixed(2)} c/u</span>
                              </div>
                            ))}
                          </div>
                        </div>
                        <div style={{ display: 'flex', justifyContent: 'space-between', borderTop: '1px solid var(--border)', paddingTop: '12px', fontSize: '13px', fontWeight: '800' }}>
                          <span>TOTAL EXTRAÍDO:</span>
                          <span style={{ color: 'var(--neon)' }}>${scanResult.totalAmount.toFixed(2)}</span>
                        </div>
                        <div style={{ background: 'rgba(0, 230, 118, 0.04)', border: '1px solid var(--neon-border)', borderRadius: '8px', padding: '10px', fontSize: '11.5px', color: 'var(--text-secondary)', lineHeight: '1.4' }}>
                          ✅ Actualización sincronizada: Total cargado a Gastos. Artículos actualizados en Despensa e Inventario.
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              )}
              {/* ── TAB: SYNC ── */}
              {activeTab === 'sync' && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '20px', maxWidth: '680px' }}>
                  {/* Connection status */}
                  <div className="card">
                    <div className="card-header">
                      <span className="card-title">Estado de Conexión</span>
                      <span className={`badge ${neonStatus === 'CONNECTED' ? 'badge-neon' : 'badge-danger'}`}>
                        {neonStatus === 'CONNECTED' ? 'ONLINE' : neonStatus === 'CONNECTING' ? 'CONECTANDO...' : 'OFFLINE'}
                      </span>
                    </div>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                      {[
                        ['Proveedor', 'Neon Serverless PostgreSQL (sa-east-1)'],
                        ['Gastos registrados', expenses.length],
                        ['Artículos en despensa', inventory.length],
                        ['Lista de compras', shopping.length],
                      ].map(([label, value]) => (
                        <div key={label} className="list-row">
                          <span style={{ fontSize: '12px', color: 'var(--text-secondary)', fontWeight: '600' }}>{label}</span>
                          <span style={{ fontSize: '12px', color: 'var(--text-primary)', fontWeight: '700' }}>{value}</span>
                        </div>
                      ))}
                    </div>
                    <button onClick={fetchData} className="btn btn-primary" style={{ marginTop: '14px', width: '100%' }} disabled={loading}>
                      <svg width="13" height="13" fill="none" stroke="currentColor" strokeWidth="2" style={{ marginRight: '6px' }}><path d="M21.5 2v6h-6M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.67"/></svg>
                      {loading ? 'Sincronizando...' : 'Sincronizar Ahora'}
                    </button>
                  </div>

                  {/* Household info */}
                  <div className="card">
                    <div className="card-header">
                      <span className="card-title">Hogar Activo</span>
                      <span className="badge badge-neon">Conectado</span>
                    </div>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                      <div className="list-row">
                        <span style={{ fontSize: '12px', color: 'var(--text-secondary)', fontWeight: '600' }}>Código del Hogar</span>
                        <span style={{ fontSize: '13px', color: 'var(--neon)', fontWeight: '800', letterSpacing: '1px' }}>{householdCode}</span>
                      </div>
                      <div className="list-row">
                        <span style={{ fontSize: '12px', color: 'var(--text-secondary)', fontWeight: '600' }}>Usuario Activo</span>
                        <span style={{ fontSize: '12px', color: 'var(--text-primary)', fontWeight: '700' }}>{currentUser}</span>
                      </div>
                    </div>
                    <button onClick={onLogout} className="btn btn-ghost" style={{ marginTop: '14px', width: '100%', color: 'var(--danger)', borderColor: 'rgba(239,83,80,0.3)' }}>
                      Cerrar Sesión
                    </button>
                  </div>

                  {/* Gemini model selector */}
                  <div className="card">
                    <div className="card-header">
                      <span className="card-title">Modelo de IA (Gemini)</span>
                      <span className="badge badge-neon">{geminiModel}</span>
                    </div>
                    <p style={{ fontSize: '12px', color: 'var(--text-secondary)', marginBottom: '14px', lineHeight: '1.5' }}>
                      Modelo usado en escaneo de tickets y detección de despensa.
                    </p>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                      {[
                        { id: 'gemini-2.5-flash', label: 'Gemini 2.5 Flash', desc: 'Estable y gratuito (recomendado)' },
                        { id: 'gemini-1.5-flash', label: 'Gemini 1.5 Flash', desc: 'Ligero y gratuito' },
                        { id: 'gemini-3.5-flash', label: 'Gemini 3.5 Flash', desc: 'Más inteligente' },
                      ].map(m => {
                        const isSelected = geminiModel === m.id;
                        return (
                          <button
                            key={m.id}
                            onClick={() => {
                              setGeminiModel(m.id);
                              if (typeof window !== 'undefined') localStorage.setItem('hogar_sincro_gemini_model', m.id);
                            }}
                            style={{
                              width: '100%', padding: '12px 16px', textAlign: 'left',
                              display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                              background: isSelected ? 'var(--neon-dim)' : 'var(--bg-elevated)',
                              border: `1px solid ${isSelected ? 'var(--neon-border)' : 'var(--border)'}`,
                              color: isSelected ? 'var(--neon)' : 'var(--text-primary)',
                              borderRadius: '8px', cursor: 'pointer', transition: 'all 0.15s',
                            }}
                          >
                            <div>
                              <div style={{ fontSize: '13px', fontWeight: '700' }}>{m.label}</div>
                              <div style={{ fontSize: '11px', color: isSelected ? 'var(--neon)' : 'var(--text-muted)', marginTop: '2px' }}>{m.desc}</div>
                            </div>
                            {isSelected && (
                              <svg width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24"><polyline points="20 6 9 17 4 12"/></svg>
                            )}
                          </button>
                        );
                      })}
                    </div>
                  </div>
                </div>
              )}
            </>
          )}
        </div>
      </main>

      {/* ── Toast Stack ── */}
      <ToastContainer toasts={toasts} onRemove={removeToast} />
    </div>
  );
}
