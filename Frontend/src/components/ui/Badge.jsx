const variants = {
  success: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  danger: 'bg-danger-50 text-danger-700 border-danger-200',
  warning: 'bg-warning-50 text-warning-700 border-warning-200',
  info: 'bg-blue-50 text-blue-700 border-blue-200',
  neutral: 'bg-slate-50 text-slate-600 border-slate-200',
};

const sizes = {
  sm: 'px-2 py-0.5 text-xs',
  md: 'px-2.5 py-1 text-sm',
};

export default function Badge({
  children,
  variant = 'neutral',
  size = 'sm',
  dot = false,
  className = '',
}) {
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full font-medium border ${variants[variant]} ${sizes[size]} ${className}`}
    >
      {dot && (
        <span
          className={`h-1.5 w-1.5 rounded-full ${
            variant === 'success'
              ? 'bg-emerald-500'
              : variant === 'danger'
              ? 'bg-danger-500'
              : variant === 'warning'
              ? 'bg-warning-500'
              : variant === 'info'
              ? 'bg-blue-500'
              : 'bg-slate-400'
          }`}
        />
      )}
      {children}
    </span>
  );
}
