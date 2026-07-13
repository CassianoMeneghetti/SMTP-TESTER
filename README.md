# SMTP Tester Pro

Aplicacao desktop profissional em Java 26 para diagnostico completo de servidores SMTP.

## Recursos

- Diagnostico de DNS MX do dominio remetente
- Teste de conectividade TCP e latencia
- Handshake SMTP com exibicao do banner
- EHLO e leitura das capacidades do servidor
- STARTTLS e SMTPS com detalhes de protocolo, cifra e certificado
- Validacao de autenticacao PLAIN e LOGIN
- Envio opcional de e-mail de teste
- Linha do tempo e log tecnico em tempo real

## Execucao

Este projeto e Java/Swing. `nvm` e usado para Node.js e nao e necessario para executar esta aplicacao.

Com Java 26 instalado, no PowerShell:

```powershell
.\run.ps1
```

Ou manualmente:

```powershell
javac -d out $(Get-ChildItem -Recurse src/main/java -Filter *.java | ForEach-Object FullName)
java -cp out br.com.smtptesterpro.Main
```

Se Maven estiver instalado:

```powershell
mvn package
java -jar target/smtp-tester-pro-1.0.0.jar
```

## Levar para outra maquina

Gere a pasta de distribuicao:

```powershell
.\package.ps1
```

Copie a pasta `dist` para a outra maquina e execute um destes arquivos:

```powershell
.\SMTP Tester Pro.ps1
```

ou:

```bat
SMTP Tester Pro.bat
```

A outra maquina precisa ter Java instalado e acessivel pelo comando `java`.

## Levar para outra maquina sem Java instalado

Gere uma distribuicao portatil com runtime Java embutido:

```powershell
.\package-portable.ps1
```

Copie a pasta abaixo para a outra maquina Windows:

```text
dist-portable\SMTP Tester Pro
```

Na maquina de destino, execute:

```text
SMTP Tester Pro.exe
```

Essa opcao nao exige Java instalado na maquina de destino, porque o runtime e empacotado junto com a aplicacao.
