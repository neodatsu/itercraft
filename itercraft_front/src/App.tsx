import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { HealthCheckPage } from './pages/healthcheck/HealthCheckPage';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/healthcheck" element={<HealthCheckPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
