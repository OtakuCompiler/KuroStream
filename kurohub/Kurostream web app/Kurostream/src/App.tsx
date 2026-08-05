import { Routes, Route } from 'react-router-dom'
import { AppShell } from '@/components/AppShell'
import Home from '@/pages/Home'
import Browse from '@/pages/Browse'
import SearchPage from '@/pages/SearchPage'
import Library from '@/pages/Library'
import TitleDetail from '@/pages/TitleDetail'
import Watch from '@/pages/Watch'
import SettingsPage from '@/pages/Settings'
import Extensions from '@/pages/Extensions'
import Downloads from '@/pages/Downloads'

export default function App() {
  return (
    <AppShell>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/browse" element={<Browse />} />
        <Route path="/search" element={<SearchPage />} />
        <Route path="/library" element={<Library />} />
        <Route path="/title/:id" element={<TitleDetail />} />
        <Route path="/watch/:id" element={<Watch />} />
        <Route path="/settings" element={<SettingsPage />} />
        <Route path="/extensions" element={<Extensions />} />
        <Route path="/downloads" element={<Downloads />} />
      </Routes>
    </AppShell>
  )
}
