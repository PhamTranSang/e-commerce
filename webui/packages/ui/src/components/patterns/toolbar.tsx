import type { ReactNode } from 'react';
import { cn } from '../../lib/utils';
import { Button } from '../ui/button';
import { IconInput } from '../ui/input';
import { Select } from '../ui/select';
import { FilterIcon, SearchIcon } from '../ui/icons';

export type FilterConfig = {
  id: string;
  value: string;
  onChange: (value: string) => void;
  options: string[];
};

type ToolbarProps = {
  filters?: FilterConfig[];
  search: string;
  onSearchChange: (value: string) => void;
  searchPlaceholder: string;
  className?: string;
  trailing?: ReactNode;
};

/** The `[selects…] [Filters] ————— [search]` strip repeated on every list screen. */
export function Toolbar({
  filters = [],
  search,
  onSearchChange,
  searchPlaceholder,
  className,
  trailing
}: ToolbarProps) {
  return (
    <div className={cn('flex flex-wrap items-center gap-3', className)}>
      {filters.map((filter) => (
        <Select
          key={filter.id}
          aria-label={filter.options[0]}
          value={filter.value}
          onChange={(event) => filter.onChange(event.target.value)}
          wrapperClassName="w-[132px]"
        >
          {filter.options.map((option) => (
            <option key={option} value={option}>
              {option}
            </option>
          ))}
        </Select>
      ))}
      <Button variant="outline" size="md" type="button" className="shrink-0 px-3">
        <FilterIcon size={15} />
        Filters
      </Button>
      <div className="ml-auto flex items-center gap-3">
        {trailing}
        <IconInput
          value={search}
          onChange={(event) => onSearchChange(event.target.value)}
          placeholder={searchPlaceholder}
          aria-label={searchPlaceholder}
          wrapperClassName="w-[184px]"
          trailing={<SearchIcon size={16} className="text-ink-subtle" />}
        />
      </div>
    </div>
  );
}
