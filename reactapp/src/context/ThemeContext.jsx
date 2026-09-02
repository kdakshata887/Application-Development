import { createContext, useContext, useEffect, useState } from 'react'

const ThemeContext = createContext(null)

export function ThemeProvider({ children }) {
  const [theme, setTheme] = useState(() => localStorage.getItem('edutrack_theme') || 'midnight')

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme)
    localStorage.setItem('edutrack_theme', theme)
  }, [theme])

  function cycleTheme() {
    setTheme((t) => (t === 'midnight' ? 'daylight' : t === 'daylight' ? 'slate' : 'midnight'))
  }

  return (
    <ThemeContext.Provider value={{ theme, setTheme, cycleTheme }}>
      {children}
    </ThemeContext.Provider>
  )
}

export function useTheme() {
  return useContext(ThemeContext)
}
