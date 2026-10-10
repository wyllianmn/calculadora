# Calculadora Web (Java)

Calculadora com **backend em Java puro** (sem bibliotecas externas) e **frontend em HTML/CSS/JavaScript**.

- O navegador envia a conta para `GET /api/calcular?expr=2%2B3*4`
- O Java calcula e responde `{"resultado":"14"}` (ou `{"erro":"..."}`)
- Suporta `+ - * /`, parênteses, `%`, números decimais e teclado

## Estrutura

```
calculadora/
├── src/Main.java        # servidor + API + avaliador de expressões
├── public/              # frontend (index.html, style.css, app.js)
├── Dockerfile           # usado para publicar online
└── README.md
```

## Rodar no seu computador

Requisito: Java 17 ou superior (`java -version`).

```bash
javac -encoding UTF-8 -d out src/Main.java
java -cp out Main
```

Abra http://localhost:8080 (rode sempre de dentro da pasta `calculadora`, pois o servidor procura a pasta `public` ali).

Para usar outra porta: `PORT=3000 java -cp out Main`

## Publicar online (Render + GitHub)

1. Crie uma conta em https://github.com e um repositório novo (ex.: `calculadora`).
2. Na pasta do projeto:
   ```bash
   git init
   git add .
   git commit -m "Calculadora"
   git branch -M main
   git remote add origin https://github.com/SEU_USUARIO/calculadora.git
   git push -u origin main
   ```
3. Crie uma conta em https://render.com (pode entrar com o GitHub).
4. **New +** → **Web Service** → escolha o repositório `calculadora`.
5. Em **Language/Runtime** selecione **Docker** (a Render detecta o `Dockerfile`).
6. Escolha o plano **Free** e clique em **Deploy**.
7. Quando terminar, a Render mostra o link, algo como `https://calculadora-xxxx.onrender.com`.

Cada `git push` novo atualiza o site automaticamente.

> Confira as condições do plano gratuito no site da Render. Em geral, planos gratuitos "dormem" após um tempo sem acessos, e a primeira visita depois disso demora para carregar.

## Domínio próprio (opcional)

Registre um domínio (ex.: em registro.br para `.com.br`) e, no painel do serviço na Render, vá em **Settings → Custom Domains** e siga as instruções de DNS.
