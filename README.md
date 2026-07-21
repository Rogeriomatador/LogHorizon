# Log Horizon — Site Oficial

Site oficial do servidor Minecraft Log Horizon, preparado para jogadores Java e Bedrock e para a distribuição do aplicativo **Log Horizon Voice**.

## Publicação

O site é publicado automaticamente pelo workflow em `.github/workflows/pages.yml`.

Na primeira publicação, abra no GitHub:

1. **Settings**
2. **Pages**
3. Em **Build and deployment > Source**, selecione **GitHub Actions**

Endereço esperado:

`https://rogeriomatador.github.io/LogHorizon/`

## Configuração

Os dados que mudam ficam no início do arquivo `script.js`:

```js
const CONFIG = {
  javaAddress: "",
  bedrockAddress: "",
  bedrockPort: "",
  discordUrl: "",
  apkUrl: "",
  apkVersion: "1.0.0"
};
```

## Publicar o aplicativo

1. Crie a pasta `downloads` no repositório.
2. Envie o APK release assinado com o nome `LogHorizonVoice-v1.0.0.apk`.
3. Configure no `script.js`:

```js
apkUrl: "downloads/LogHorizonVoice-v1.0.0.apk"
```

Nunca publique o arquivo `.jks`, suas senhas ou o APK de debug.

## Arquivos principais

- `index.html`: conteúdo do site
- `styles.css`: identidade visual e responsividade
- `script.js`: interações e dados do servidor
- `favicon.svg`: ícone do site
- `.github/workflows/pages.yml`: publicação automática
