import { useTranslations } from 'next-intl';
import { RegistrationWizard } from "@/components/register/registration-wizard"

export default function RegisterPage() {
  const t = useTranslations('Register');

  return (
    <div className="flex flex-col items-center justify-center gap-8 py-8">
      <div className="text-center space-y-2">
        <h1 className="text-3xl font-bold tracking-tight">{t('pageTitle')}</h1>
        <p className="text-muted-foreground max-w-md">
          {t('pageDescription')}
        </p>
      </div>
      <RegistrationWizard />
    </div>
  )
}
