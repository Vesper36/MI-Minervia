import { MarketingLayout } from '@/components/marketing';

export default function MarketingRouteLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return <MarketingLayout>{children}</MarketingLayout>;
}
