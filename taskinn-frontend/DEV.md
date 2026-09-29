# Typescript

### Code
- group public, private and protected fields logically
- avoid `any` and `unknown` unless justified


### Prefix

- in `angular.json` set the component prefix to **tuw**. Have all selectors follow the form **'tuw-spinner'**.

### Dependency Injection

- inject dependencies as `private readonly` using `inject` from `@angular/core`, instead of constructor injection. For example:

```typescript
export class Loginpage {
  private readonly authService = inject(AuthService);
}
```

### Input and Output

- use the new **[Signals Component Api](https://blog.angular-university.io/angular-signal-components/)**. For example:

```typescript
export class BookComponent {
  @Input({ required = true }) // instead of this
  book = input.required<Book>(); // do this
}
```

## Routes

- use lazy loading for major routes. For example:

```typescript
{
  path: 'auth',
  loadChildren: () => import('auth.routes').then((auth) => auth.AUTH_ROUTES),
},
{
  path: 'landing-page',
  loadComponent: () => import('kanding-page').then((m) => m.LandingPageComponent),
}
```

- use [Functional Guards](https://angular.dev/guide/routing/route-guards) for authorizing routes. For example:

```typescript
export const authGuard: CanActivateFn = (
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot
) => {
  const authService = inject(AuthService);
  return authService.isAuthenticated();
};
```

## Services
- make services stateless, without any side-effects, they are simple fetch utilities. Any logic is implemented in either the component or a store.


# HTML

- use the new [Angular Control Flow](https://angular.dev/guide/templates/control-flow). For example:

```html
<div>
    @if(userSignedIn) {
        <button>Sign out<button>
    } @else {
        <button>Log in</button>
    }
</div>
```
