import { useState, useEffect } from 'react';
import Login from './components/Login';
import Dashboard from './components/Dashboard';
import { getToken, clearToken } from './services/api';

function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  useEffect(() => {
    if (getToken()) {
      setIsAuthenticated(true);
    }
  }, []);

  const handleLogout = () => {
    clearToken();
    setIsAuthenticated(false);
  };

  if (!isAuthenticated) {
    return <Login onLoginComplete={() => setIsAuthenticated(true)} />;
  }

  return <Dashboard onLogout={handleLogout} />;
}

export default App;
