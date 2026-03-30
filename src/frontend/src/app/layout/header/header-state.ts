export type HeaderNavItem = Readonly<{
  label: string;
  routerLink: string;
  fragment?: string;
}>;

export type HeaderState = Readonly<{
  logoText: string;
  navItems: readonly HeaderNavItem[];
  showAction: boolean;
}>;

export const LANDING_HEADER_STATE: HeaderState = {
  logoText: 'ActorsHub',
  navItems: [
    { label: 'Актёры', routerLink: '/search' },
    { label: 'О платформе', routerLink: '/', fragment: 'about' },
    { label: 'Для кого', routerLink: '/', fragment: 'audience' },
  ],
  showAction: true,
};

export const SEARCH_HEADER_STATE: HeaderState = {
  logoText: 'ActorsHub',
  navItems: [{ label: 'Главная', routerLink: '/' }],
  showAction: true,
};

export const PROFILE_HEADER_STATE: HeaderState = {
  logoText: 'ActorsHub',
  navItems: [
    { label: 'Главная', routerLink: '/' },
    { label: 'Актёры', routerLink: '/search' },
  ],
  showAction: true,
};

export const AUTH_HEADER_STATE: HeaderState = {
  logoText: 'ActorsHub',
  navItems: [],
  showAction: false,
};

export const DEFAULT_HEADER_STATE = LANDING_HEADER_STATE;
