import { useState } from 'react';
import { cn, ChevronDownIcon, ChevronRightIcon, FolderIcon, MoreVerticalIcon } from '@ui';
import type { CategoryTreeResponse } from '@domain/index';

type CategoryTreeProps = {
  nodes: CategoryTreeResponse[];
  selectedId?: string;
  onSelect?: (node: CategoryTreeResponse) => void;
  /** The dashboard variant prints the raw id next to each name. */
  showIds?: boolean;
  filter?: string;
  className?: string;
};

function matches(node: CategoryTreeResponse, filter: string): boolean {
  if (!filter) return true;
  const needle = filter.toLowerCase();
  if (node.categoryName.toLowerCase().includes(needle)) return true;
  return (node.children ?? []).some((child) => matches(child, needle));
}

export function CategoryTree({ nodes, selectedId, onSelect, showIds, filter = '', className }: CategoryTreeProps) {
  const [collapsed, setCollapsed] = useState<string[]>([]);

  const toggle = (id: string) =>
    setCollapsed((current) => (current.includes(id) ? current.filter((item) => item !== id) : [...current, id]));

  const renderNode = (node: CategoryTreeResponse, depth: number) => {
    if (!matches(node, filter)) return null;
    const hasChildren = Boolean(node.children?.length);
    const isOpen = !collapsed.includes(node.categoryId) || Boolean(filter);
    const isSelected = node.categoryId === selectedId;

    return (
      <li key={node.categoryId}>
        <div
          className={cn(
            'group flex items-center gap-1.5 rounded-lg py-1.5 pr-1.5 transition-colors',
            isSelected ? 'bg-brand-soft' : 'hover:bg-surface-muted'
          )}
          style={{ paddingLeft: 4 + depth * 20 }}
        >
          {hasChildren ? (
            <button
              type="button"
              onClick={() => toggle(node.categoryId)}
              aria-label={isOpen ? `Collapse ${node.categoryName}` : `Expand ${node.categoryName}`}
              aria-expanded={isOpen}
              className="rounded p-0.5 text-ink-subtle hover:text-ink"
            >
              {isOpen ? <ChevronDownIcon size={14} /> : <ChevronRightIcon size={14} />}
            </button>
          ) : (
            <span className="w-[22px]" />
          )}
          <button
            type="button"
            onClick={() => onSelect?.(node)}
            className="flex min-w-0 flex-1 items-center gap-2 text-left"
          >
            <FolderIcon size={16} className="text-ink-subtle" />
            <span className={cn('truncate text-sm', isSelected ? 'font-medium text-ink' : 'text-ink')}>
              {node.categoryName}
            </span>
            {showIds ? <span className="truncate text-xs text-ink-subtle">({node.categoryId})</span> : null}
          </button>
          <button
            type="button"
            aria-label={`Actions for ${node.categoryName}`}
            className="rounded p-0.5 text-ink-subtle opacity-0 transition-opacity group-hover:opacity-100"
          >
            <MoreVerticalIcon size={14} />
          </button>
        </div>
        {hasChildren && isOpen ? <ul>{node.children.map((child) => renderNode(child, depth + 1))}</ul> : null}
      </li>
    );
  };

  return <ul className={cn('space-y-0.5', className)}>{nodes.map((node) => renderNode(node, 0))}</ul>;
}

export function TreeLegend({ total, className }: { total?: number; className?: string }) {
  return (
    <div className={cn('flex items-center justify-between text-xs text-ink-muted', className)}>
      <div className="flex items-center gap-4">
        <span className="flex items-center gap-1.5">
          <span className="h-2 w-2 rounded-full bg-emerald-500" /> Active
        </span>
        <span className="flex items-center gap-1.5">
          <span className="h-2 w-2 rounded-full bg-amber-500" /> Inactive
        </span>
      </div>
      {total !== undefined ? <span>Total Categories: {total}</span> : null}
    </div>
  );
}
