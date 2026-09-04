import { Badge } from '../ui/badge';

export function StatusBadge({ isActive, className }: { isActive: boolean; className?: string }) {
  return (
    <Badge tone={isActive ? 'success' : 'neutral'} className={className}>
      {isActive ? 'Active' : 'Inactive'}
    </Badge>
  );
}
