import { describe, it, expect, beforeAll } from 'vitest'
import fs from 'fs'
import path from 'path'

interface TranslationObject {
  [key: string]: string | TranslationObject
}

describe('i18n Completeness Tests (PBT-MARKETING-02)', () => {
  let enMessages: TranslationObject
  let plMessages: TranslationObject
  let zhCNMessages: TranslationObject

  beforeAll(() => {
    const messagesDir = path.resolve(__dirname, '../../messages')
    enMessages = JSON.parse(fs.readFileSync(path.join(messagesDir, 'en.json'), 'utf-8'))
    plMessages = JSON.parse(fs.readFileSync(path.join(messagesDir, 'pl.json'), 'utf-8'))
    zhCNMessages = JSON.parse(fs.readFileSync(path.join(messagesDir, 'zh-CN.json'), 'utf-8'))
  })

  function getAllKeys(obj: TranslationObject, prefix = ''): string[] {
    const keys: string[] = []
    for (const key of Object.keys(obj)) {
      const fullKey = prefix ? `${prefix}.${key}` : key
      const value = obj[key]
      if (typeof value === 'object' && value !== null) {
        keys.push(...getAllKeys(value as TranslationObject, fullKey))
      } else {
        keys.push(fullKey)
      }
    }
    return keys
  }

  function getValueByPath(obj: TranslationObject, path: string): string | undefined {
    const parts = path.split('.')
    let current: TranslationObject | string = obj
    for (const part of parts) {
      if (typeof current !== 'object' || current === null) {
        return undefined
      }
      current = current[part]
    }
    return typeof current === 'string' ? current : undefined
  }

  describe('Marketing namespace completeness', () => {
    it('pl.json has all Marketing keys from en.json', () => {
      const enKeys = getAllKeys(enMessages).filter(k => k.startsWith('Marketing.'))
      const plKeys = getAllKeys(plMessages).filter(k => k.startsWith('Marketing.'))

      const missingInPl = enKeys.filter(k => !plKeys.includes(k))
      expect(missingInPl).toEqual([])
    })

    it('zh-CN.json has all Marketing keys from en.json', () => {
      const enKeys = getAllKeys(enMessages).filter(k => k.startsWith('Marketing.'))
      const zhKeys = getAllKeys(zhCNMessages).filter(k => k.startsWith('Marketing.'))

      const missingInZh = enKeys.filter(k => !zhKeys.includes(k))
      expect(missingInZh).toEqual([])
    })
  })

  describe('Portal namespace completeness', () => {
    it('pl.json has all Portal keys from en.json', () => {
      const enKeys = getAllKeys(enMessages).filter(k => k.startsWith('Portal.'))
      const plKeys = getAllKeys(plMessages).filter(k => k.startsWith('Portal.'))

      const missingInPl = enKeys.filter(k => !plKeys.includes(k))
      expect(missingInPl).toEqual([])
    })

    it('zh-CN.json has all Portal keys from en.json', () => {
      const enKeys = getAllKeys(enMessages).filter(k => k.startsWith('Portal.'))
      const zhKeys = getAllKeys(zhCNMessages).filter(k => k.startsWith('Portal.'))

      const missingInZh = enKeys.filter(k => !zhKeys.includes(k))
      expect(missingInZh).toEqual([])
    })
  })

  describe('Register namespace completeness', () => {
    it('pl.json has all Register keys from en.json', () => {
      const enKeys = getAllKeys(enMessages).filter(k => k.startsWith('Register.'))
      const plKeys = getAllKeys(plMessages).filter(k => k.startsWith('Register.'))

      const missingInPl = enKeys.filter(k => !plKeys.includes(k))
      expect(missingInPl).toEqual([])
    })

    it('zh-CN.json has all Register keys from en.json', () => {
      const enKeys = getAllKeys(enMessages).filter(k => k.startsWith('Register.'))
      const zhKeys = getAllKeys(zhCNMessages).filter(k => k.startsWith('Register.'))

      const missingInZh = enKeys.filter(k => !zhKeys.includes(k))
      expect(missingInZh).toEqual([])
    })
  })

  describe('All namespaces completeness', () => {
    it('pl.json has all keys from en.json', () => {
      const enKeys = getAllKeys(enMessages)
      const plKeys = getAllKeys(plMessages)

      const missingInPl = enKeys.filter(k => !plKeys.includes(k))
      expect(missingInPl).toEqual([])
    })

    it('zh-CN.json has all keys from en.json', () => {
      const enKeys = getAllKeys(enMessages)
      const zhKeys = getAllKeys(zhCNMessages)

      const missingInZh = enKeys.filter(k => !zhKeys.includes(k))
      expect(missingInZh).toEqual([])
    })

    it('en.json has all keys from pl.json (no extra keys in pl)', () => {
      const enKeys = getAllKeys(enMessages)
      const plKeys = getAllKeys(plMessages)

      const extraInPl = plKeys.filter(k => !enKeys.includes(k))
      expect(extraInPl).toEqual([])
    })

    it('en.json has all keys from zh-CN.json (no extra keys in zh-CN)', () => {
      const enKeys = getAllKeys(enMessages)
      const zhKeys = getAllKeys(zhCNMessages)

      const extraInZh = zhKeys.filter(k => !enKeys.includes(k))
      expect(extraInZh).toEqual([])
    })
  })

  describe('Translation values are non-empty', () => {
    it('all en.json values are non-empty strings', () => {
      const enKeys = getAllKeys(enMessages)
      const emptyKeys = enKeys.filter(k => {
        const value = getValueByPath(enMessages, k)
        return !value || value.trim() === ''
      })
      expect(emptyKeys).toEqual([])
    })

    it('all pl.json values are non-empty strings', () => {
      const plKeys = getAllKeys(plMessages)
      const emptyKeys = plKeys.filter(k => {
        const value = getValueByPath(plMessages, k)
        return !value || value.trim() === ''
      })
      expect(emptyKeys).toEqual([])
    })

    it('all zh-CN.json values are non-empty strings', () => {
      const zhKeys = getAllKeys(zhCNMessages)
      const emptyKeys = zhKeys.filter(k => {
        const value = getValueByPath(zhCNMessages, k)
        return !value || value.trim() === ''
      })
      expect(emptyKeys).toEqual([])
    })
  })

  describe('Placeholder consistency', () => {
    it('pl.json preserves placeholders from en.json', () => {
      const enKeys = getAllKeys(enMessages)
      const placeholderPattern = /\{[^}]+\}/g

      const inconsistentKeys: string[] = []
      for (const key of enKeys) {
        const enValue = getValueByPath(enMessages, key)
        const plValue = getValueByPath(plMessages, key)

        if (enValue && plValue) {
          const enPlaceholders = enValue.match(placeholderPattern) || []
          const plPlaceholders = plValue.match(placeholderPattern) || []

          if (enPlaceholders.sort().join(',') !== plPlaceholders.sort().join(',')) {
            inconsistentKeys.push(key)
          }
        }
      }
      expect(inconsistentKeys).toEqual([])
    })

    it('zh-CN.json preserves placeholders from en.json', () => {
      const enKeys = getAllKeys(enMessages)
      const placeholderPattern = /\{[^}]+\}/g

      const inconsistentKeys: string[] = []
      for (const key of enKeys) {
        const enValue = getValueByPath(enMessages, key)
        const zhValue = getValueByPath(zhCNMessages, key)

        if (enValue && zhValue) {
          const enPlaceholders = enValue.match(placeholderPattern) || []
          const zhPlaceholders = zhValue.match(placeholderPattern) || []

          if (enPlaceholders.sort().join(',') !== zhPlaceholders.sort().join(',')) {
            inconsistentKeys.push(key)
          }
        }
      }
      expect(inconsistentKeys).toEqual([])
    })
  })

  describe('Marketing specific keys', () => {
    const requiredMarketingKeys = [
      'Marketing.nav.home',
      'Marketing.nav.about',
      'Marketing.nav.programs',
      'Marketing.nav.admissions',
      'Marketing.home.title',
      'Marketing.home.heroTitle',
      'Marketing.home.cta',
      'Marketing.about.title',
      'Marketing.about.mission',
      'Marketing.programs.title',
      'Marketing.admissions.title',
    ]

    it.each(requiredMarketingKeys)('en.json has key: %s', (key) => {
      const value = getValueByPath(enMessages, key)
      expect(value).toBeDefined()
      expect(value).not.toBe('')
    })

    it.each(requiredMarketingKeys)('pl.json has key: %s', (key) => {
      const value = getValueByPath(plMessages, key)
      expect(value).toBeDefined()
      expect(value).not.toBe('')
    })

    it.each(requiredMarketingKeys)('zh-CN.json has key: %s', (key) => {
      const value = getValueByPath(zhCNMessages, key)
      expect(value).toBeDefined()
      expect(value).not.toBe('')
    })
  })

  describe('Portal specific keys', () => {
    const requiredPortalKeys = [
      'Portal.sidebar.title',
      'Portal.sidebar.dashboard',
      'Portal.sidebar.profile',
      'Portal.sidebar.courses',
      'Portal.sidebar.logout',
      'Portal.login.title',
      'Portal.login.email',
      'Portal.login.password',
      'Portal.login.submit',
      'Portal.dashboard.welcome',
      'Portal.profile.title',
      'Portal.courses.title',
    ]

    it.each(requiredPortalKeys)('en.json has key: %s', (key) => {
      const value = getValueByPath(enMessages, key)
      expect(value).toBeDefined()
      expect(value).not.toBe('')
    })

    it.each(requiredPortalKeys)('pl.json has key: %s', (key) => {
      const value = getValueByPath(plMessages, key)
      expect(value).toBeDefined()
      expect(value).not.toBe('')
    })

    it.each(requiredPortalKeys)('zh-CN.json has key: %s', (key) => {
      const value = getValueByPath(zhCNMessages, key)
      expect(value).toBeDefined()
      expect(value).not.toBe('')
    })
  })
})
