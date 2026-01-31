import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './auth/AuthProvider';
import { Header } from './components/common/Header';
import { Breadcrumb } from './components/common/Breadcrumb';
import { Footer } from './components/common/Footer';
import { HomePage } from './pages/home/HomePage';
import { HealthCheckPage } from './pages/healthcheck/HealthCheckPage';
import { DashboardPage } from './pages/dashboard/DashboardPage';
import { ProtectedRoute } from './auth/ProtectedRoute';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/healthcheck" element={<HealthCheckPage />} />
        <Route path="*" element={
          <AuthProvider>
            <Header />
            <Breadcrumb />
            <main className="app-content">
              <Routes>
                <Route path="/" element={<HomePage />} />
                <Route path="/dashboard" element={
                  <ProtectedRoute>
                    <DashboardPage />
                  </ProtectedRoute>
                } />
              </Routes>
            </main>
            <Footer />
          </AuthProvider>
        } />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
