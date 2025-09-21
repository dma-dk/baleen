/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{html,ts}",
  ],
  theme: {
    extend: {
      colors: {
        // Use PrimeNG semantic color variables
        primary: {
          50: 'rgb(var(--p-primary-50))',
          100: 'rgb(var(--p-primary-100))',
          200: 'rgb(var(--p-primary-200))',
          300: 'rgb(var(--p-primary-300))',
          400: 'rgb(var(--p-primary-400))',
          500: 'rgb(var(--p-primary-500))',
          600: 'rgb(var(--p-primary-600))',
          700: 'rgb(var(--p-primary-700))',
          800: 'rgb(var(--p-primary-800))',
          900: 'rgb(var(--p-primary-900))',
          950: 'rgb(var(--p-primary-950))',
        },
        surface: {
          0: 'rgb(var(--p-surface-0))',
          50: 'rgb(var(--p-surface-50))',
          100: 'rgb(var(--p-surface-100))',
          200: 'rgb(var(--p-surface-200))',
          300: 'rgb(var(--p-surface-300))',
          400: 'rgb(var(--p-surface-400))',
          500: 'rgb(var(--p-surface-500))',
          600: 'rgb(var(--p-surface-600))',
          700: 'rgb(var(--p-surface-700))',
          800: 'rgb(var(--p-surface-800))',
          900: 'rgb(var(--p-surface-900))',
          950: 'rgb(var(--p-surface-950))',
        }
      },
      fontFamily: {
        sans: ['-apple-system', 'BlinkMacSystemFont', '"Segoe UI"', 'Roboto', '"Helvetica Neue"', 'Arial', 'sans-serif'],
        mono: ['monospace'],
      },
      spacing: {
        '18': '4.5rem',
        '72': '18rem',
        '84': '21rem',
        '96': '24rem',
      }
    },
  },
  plugins: [],
  // Disable Tailwind's preflight to avoid conflicts with PrimeNG
  corePlugins: {
    preflight: false,
  }
}