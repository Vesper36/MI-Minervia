'use client';

import { useState } from 'react';
import { useTranslations } from 'next-intl';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Button } from '@/components/ui/button';

export default function StudentPortalPage() {
  const [isLoading, setIsLoading] = useState(false);
  const [showMaintenance, setShowMaintenance] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setTimeout(() => {
      setIsLoading(false);
      setShowMaintenance(true);
    }, 1500);
  };

  if (showMaintenance) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center py-16">
        <Card className="w-full max-w-md border-0 shadow-lg">
          <CardContent className="pt-6 text-center">
            <div className="w-16 h-16 rounded-full bg-amber-100 flex items-center justify-center mx-auto mb-4">
              <span className="text-2xl">!</span>
            </div>
            <h2 className="text-xl font-bold mb-2">System Maintenance</h2>
            <p className="text-muted-foreground mb-4">
              The Student Portal is currently undergoing scheduled maintenance.
              We apologize for any inconvenience.
            </p>
            <p className="text-sm text-muted-foreground">
              Expected completion: Within 24 hours
            </p>
            <p className="text-sm text-muted-foreground mt-4">
              For urgent matters, please contact:<br />
              <a href="mailto:support@minervia.edu.pl" className="text-slate-900 hover:underline">
                support@minervia.edu.pl
              </a>
            </p>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="min-h-[60vh] flex items-center justify-center py-16">
      <Card className="w-full max-w-md border-0 shadow-lg">
        <CardHeader className="text-center">
          <CardTitle className="text-2xl">Student Portal</CardTitle>
          <CardDescription>
            Sign in to access your student account, grades, and course materials.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="studentId">Student ID or Email</Label>
              <Input
                id="studentId"
                type="text"
                placeholder="e.g., 2024001234 or name@student.minervia.edu.pl"
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="password">Password</Label>
              <Input
                id="password"
                type="password"
                placeholder="Enter your password"
                required
              />
            </div>
            <div className="flex items-center justify-between text-sm">
              <label className="flex items-center gap-2">
                <input type="checkbox" className="rounded" />
                <span className="text-muted-foreground">Remember me</span>
              </label>
              <a href="#" className="text-slate-900 hover:underline">
                Forgot password?
              </a>
            </div>
            <Button type="submit" className="w-full" disabled={isLoading}>
              {isLoading ? 'Signing in...' : 'Sign In'}
            </Button>
          </form>
          <div className="mt-6 pt-6 border-t text-center text-sm text-muted-foreground">
            <p>
              New student? Your login credentials were sent to your email after enrollment confirmation.
            </p>
            <p className="mt-2">
              Need help? Contact{' '}
              <a href="mailto:it-support@minervia.edu.pl" className="text-slate-900 hover:underline">
                IT Support
              </a>
            </p>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
