import { createContext, useContext, useState } from 'react'
import { api } from '../api/client'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const raw = localStorage.getItem('edutrack_user')
    return raw ? JSON.parse(raw) : null
  })

  async function login(username, password) {
    const data = await api.login(username, password)
    localStorage.setItem('edutrack_token', data.token)
    const userInfo = { userId: data.userId, username: data.username, role: data.role }
    localStorage.setItem('edutrack_user', JSON.stringify(userInfo))
    setUser(userInfo)
    return userInfo
  }

  function logout() {
    localStorage.removeItem('edutrack_token')
    localStorage.removeItem('edutrack_user')
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
