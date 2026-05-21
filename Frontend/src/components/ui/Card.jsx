export default function Card({
  children,
  className = '',
  padding = true,
  hover = false,
  ...props
}) {
  return (
    <div
      className={`bg-white rounded-xl border border-slate-200 shadow-sm ${
        padding ? 'p-5' : ''
      } ${hover ? 'hover:shadow-md hover:border-slate-300 transition-all duration-200' : ''} ${className}`}
      {...props}
    >
      {children}
    </div>
  );
}
