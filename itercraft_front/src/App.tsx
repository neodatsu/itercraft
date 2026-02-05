import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './auth/AuthProvider';
import { Header } from './components/common/Header';
import { Breadcrumb } from './components/common/Breadcrumb';
import { Footer } from './components/common/Footer';
import { HomePage } from './pages/home/HomePage';
import { HealthCheckPage } from './pages/healthcheck/HealthCheckPage';
import { DashboardPage } from './pages/dashboard/DashboardPage';
import { CookiePolicyPage } from './pages/cookies/CookiePolicyPage';
import { MentionsLegalesPage } from './pages/legal/MentionsLegalesPage';
import { PrivacyPolicyPage } from './pages/legal/PrivacyPolicyPage';
import { CookieConsent } from './components/common/CookieConsent';
import { ArchitecturePage } from './pages/architecture/ArchitecturePage';
import { SseDiagramsPage } from './pages/sse/SseDiagramsPage';
import { ActivitiesPage } from './pages/activities/ActivitiesPage';
import { ResiliencePage } from './pages/resilience/ResiliencePage';
import { LudothequePage } from './pages/ludotheque/LudothequePage';
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
                <Route path="/cookies" element={<CookiePolicyPage />} />
                <Route path="/mentions-legales" element={<MentionsLegalesPage />} />
                <Route path="/confidentialite" element={<PrivacyPolicyPage />} />
                <Route path="/architecture" element={<ArchitecturePage />} />
                <Route path="/sse" element={<SseDiagramsPage />} />
                <Route path="/resilience" element={<ResiliencePage />} />
                <Route path="/activites" element={
                  <ProtectedRoute>
                    <ActivitiesPage />
                  </ProtectedRoute>
                } />
                <Route path="/ludotheque" element={
                  <ProtectedRoute>
                    <LudothequePage />
                  </ProtectedRoute>
                } />
              </Routes>
            </main>
            <Footer />
            <CookieConsent />
          </AuthProvider>
        } />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
