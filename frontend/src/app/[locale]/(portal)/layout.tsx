import * as React from "react"

export default function PortalLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <div className="min-h-screen flex flex-col bg-slate-50 dark:bg-slate-950">
      <header className="border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
        <div className="container flex h-14 items-center">
          <a className="flex items-center space-x-2" href="/">
            <span className="font-bold">Minervia Institute</span>
          </a>
        </div>
      </header>
      <main className="flex-1 container py-10">
        {children}
      </main>
      <footer className="border-t py-6">
        <div className="container text-center text-sm text-muted-foreground">
          Minervia Institute Educational Platform
        </div>
      </footer>
    </div>
  )
}
