import { useState, useCallback } from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Sidebar from './components/Sidebar';
import Topbar from './components/Topbar';
import DashboardScreen from './features/dashboard/DashboardScreen';
import InvestigatorScreen from './features/visual-investigator/InvestigatorScreen';
import OperationsScreen from './features/operations/OperationsScreen';

export default function App() {
  const [searchResults, setSearchResults] = useState(null);

  const handleSearchResult = useCallback((results) => {
    setSearchResults(results);
  }, []);

  return (
    <BrowserRouter>
      <div className="min-h-screen bg-white">
        <Sidebar />
        <div className="ml-64">
          <Topbar onSearchResult={handleSearchResult} />
          <main className="p-6">
            <Routes>
              <Route path="/" element={<DashboardScreen />} />
              <Route
                path="/investigador"
                element={<InvestigatorScreen searchResults={searchResults} />}
              />
              <Route path="/operaciones" element={<OperationsScreen />} />
            </Routes>
          </main>
        </div>
      </div>
    </BrowserRouter>
  );
}
