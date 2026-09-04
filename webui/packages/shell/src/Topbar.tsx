import { useEffect, useRef, useState } from 'react';
import { IconInput, BellIcon, ChevronDownIcon, HelpCircleIcon, LogOutIcon, MenuIcon, SearchIcon } from '@ui';

type TopbarAccount = { fullName: string; email?: string } | null;

type TopbarProps = {
  searchPlaceholder: string;
  account: TopbarAccount;
  onSignOut: () => void;
  onToggleSidebar: () => void;
};

export function Topbar({ searchPlaceholder, account, onSignOut, onToggleSidebar }: TopbarProps) {
  const displayName = account?.fullName ?? 'Admin';
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!menuOpen) return;
    const onClick = (event: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) setMenuOpen(false);
    };
    document.addEventListener('mousedown', onClick);
    return () => document.removeEventListener('mousedown', onClick);
  }, [menuOpen]);

  return (
    <header className="flex h-[62px] shrink-0 items-center gap-4 border-b border-line bg-white px-6">
      <button
        type="button"
        aria-label="Toggle navigation"
        onClick={onToggleSidebar}
        className="rounded-md p-1 text-ink-muted transition-colors hover:bg-surface-muted hover:text-ink"
      >
        <MenuIcon size={20} />
      </button>
      <IconInput
        placeholder={searchPlaceholder}
        aria-label="Global search"
        wrapperClassName="w-[430px] max-w-[45vw]"
        className="h-9 rounded-lg bg-white"
        trailing={<SearchIcon size={16} className="text-ink-subtle" />}
      />
      <div className="ml-auto flex items-center gap-4">
        <button type="button" aria-label="Help" className="text-ink-subtle hover:text-ink">
          <HelpCircleIcon size={20} />
        </button>
        <button type="button" aria-label="Notifications" className="relative text-ink-subtle hover:text-ink">
          <BellIcon size={20} />
          <span className="absolute -right-1.5 -top-1.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-brand px-1 text-[10px] font-semibold text-white">
            3
          </span>
        </button>
        <div className="relative border-l border-line pl-4" ref={menuRef}>
          <button
            type="button"
            onClick={() => setMenuOpen((value) => !value)}
            aria-haspopup="menu"
            aria-expanded={menuOpen}
            className="flex items-center gap-2.5 rounded-lg py-1 transition-colors hover:opacity-90"
          >
            <span className="text-sm text-ink-muted">
              Signed in as <span className="font-semibold text-ink">{displayName}</span>
            </span>
            <span className="flex h-8 w-8 items-center justify-center rounded-full bg-brand text-sm font-semibold text-white">
              {displayName.charAt(0).toUpperCase()}
            </span>
            <ChevronDownIcon size={15} className="text-ink-subtle" />
          </button>
          {menuOpen ? (
            <div
              role="menu"
              className="absolute right-0 top-full z-20 mt-2 w-52 overflow-hidden rounded-lg border border-line bg-white py-1 shadow-panel"
            >
              {account?.email ? (
                <p className="truncate border-b border-line px-4 py-2 text-xs text-ink-subtle">{account.email}</p>
              ) : null}
              <button
                type="button"
                role="menuitem"
                onClick={() => {
                  setMenuOpen(false);
                  onSignOut();
                }}
                className="flex w-full items-center gap-2.5 px-4 py-2.5 text-left text-sm text-ink transition-colors hover:bg-surface-muted"
              >
                <LogOutIcon size={16} className="text-ink-muted" />
                Sign out
              </button>
            </div>
          ) : null}
        </div>
      </div>
    </header>
  );
}
