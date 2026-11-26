-- Adds an optional hint per mission. Run ONCE in pgAdmin (Query Tool) on the "codequest" database.
ALTER TABLE "Mission" ADD COLUMN IF NOT EXISTS "Indice" TEXT;

UPDATE "Mission" SET "Indice" = 'input() renvoie du texte : transforme-le avec int(). Ensuite print(a + b).' WHERE "Id" = 1;
UPDATE "Mission" SET "Indice" = 'Un nombre est pair quand n % 2 == 0. Utilise if / else puis print("pair") ou print("impair").' WHERE "Id" = 2;
UPDATE "Mission" SET "Indice" = 'Compare avec if / elif, ou utilise directement la fonction max(a, b, c).' WHERE "Id" = 3;
UPDATE "Mission" SET "Indice" = 'Pars de resultat = 1, puis dans une boucle for i in range(2, n + 1) multiplie resultat par i. Pour n = 0 la réponse est 1.' WHERE "Id" = 4;
UPDATE "Mission" SET "Indice" = 'Passe le mot en minuscules avec s.lower(), puis parcours chaque lettre et compte celles qui sont dans "aeiou".' WHERE "Id" = 5;
UPDATE "Mission" SET "Indice" = 'Si n < 2 ce n''est pas premier. Sinon teste les diviseurs de 2 à racine de n : si n % d == 0 pour un d, la réponse est "non".' WHERE "Id" = 6;
