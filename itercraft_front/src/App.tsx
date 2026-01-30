import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './auth/AuthProvider';
import { Header } from './components/common/Header';
import { PublicHeader } from './components/common/PublicHeader';
import { Footer } from './components/common/Footer';
import { HomePage } from './pages/home/HomePage';
import { HealthCheckPage } from './pages/healthcheck/HealthCheckPage';
import { DashboardPage } from './pages/dashboard/DashboardPage';
import { ProtectedRoute } from './auth/ProtectedRoute';

function PublicLayout() {
  return (
    <>
      <PublicHeader />
      <main className="app-content">
        <Routes>
          <Route path="/" element={<HomePage />} />
        </Routes>
      </main>
      <Footer />
    </>
  );
}

function AuthenticatedLayout() {
  return (
    <AuthProvider>
      <Header />
      <main className="app-content">
        <Routes>
          <Route path="/dashboard" element={
            <ProtectedRoute>
              <DashboardPage />
            </ProtectedRoute>
          } />
        </Routes>
      </main>
      <Footer />
    </AuthProvider>
  );
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/healthcheck" element={<HealthCheckPage />} />
        <Route path="/" element={<PublicLayout />} />
        <Route path="*" element={<AuthenticatedLayout />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
