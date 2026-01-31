import * as React from "react"
import { StudentAuthProvider } from "@/lib/student-auth-context"

export default function PortalLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <StudentAuthProvider>
      {children}
    </StudentAuthProvider>
  )
}
