/**
 * Source catalog: Brazilian Portuguese, the default language. Every other catalog has exactly these keys
 * (`Catalog`), which the type check enforces. `{{name}}` is an interpolated value; `<anchor>` and `<nowrap>` are
 * elements filled in by `<Trans>`.
 */
export const ptBR = {
  common: {
    loading: 'Carregando...',
    demoBanner: 'Ambiente de demonstração, sem dinheiro real',
  },
  language: {
    label: 'Idioma',
  },
  nav: {
    main: 'Principal',
  },
  home: {
    tagline: 'Carteira digital com idempotência, circuit breaker, rate limit e segurança.',
    createAccount: 'Criar conta',
    signIn: 'Entrar',
    demoInbox: 'Caixa de entrada demo',
  },
  apiStatus: {
    label: 'API:',
    LOADING: 'Verificando...',
    UP: 'Online',
    DOWN: 'Fora do ar',
    UNKNOWN: 'Indisponível',
  },
  notFound: {
    title: 'Página não encontrada',
    description: 'O endereço acessado não existe.',
    backHome: 'Voltar ao início',
  },
  register: {
    title: 'Criar conta',
    intro:
      'Use dados fictícios: este é um ambiente de demonstração. Você cria a senha ao ativar a conta pelo link enviado por e-mail.',
    formLabel: 'Cadastro',
    name: 'Nome completo',
    email: 'E-mail',
    cpf: 'CPF',
    cpfHint: 'Gere um CPF fictício válido para testar.',
    submit: 'Criar conta',
    submitting: 'Enviando...',
    haveAccount: 'Já tem conta?',
    signIn: 'Entrar',
    checkEmail: 'Verifique seu e-mail',
    demoInboxNotice:
      'Neste ambiente de demonstração, os e-mails chegam na <anchor>caixa de entrada demo</anchor>.',
  },
  confirm: {
    title: 'Ativação da conta',
    linkErrorHint:
      'Peça um novo link fazendo o cadastro de novo com o mesmo <nowrap>e-mail</nowrap>.',
    backToRegister: 'Voltar ao cadastro',
    success:
      'Conta ativada. Você já pode entrar com o seu <nowrap>e-mail</nowrap> e a senha que acabou de criar.',
    goToLogin: 'Ir para o login',
    loadingSummary: 'Carregando os dados do cadastro...',
    retry: 'Tentar de novo',
    retrying: 'Tentando...',
    intro: 'Confira os dados do cadastro e crie a senha da sua conta.',
    summaryTitle: 'Estes são os seus dados?',
    name: 'Nome',
    cpf: 'CPF',
    notYours:
      'Não são os seus? Não continue: <anchor>faça o cadastro de novo</anchor>. Você vai receber um novo link, e este deixará de funcionar.',
    password: 'Crie uma senha',
    passwordHint: 'De 8 a 72 caracteres.',
    passwordConfirmation: 'Repita a senha',
    submit: 'Ativar conta',
    submitting: 'Ativando...',
  },
  login: {
    title: 'Entrar',
    intro: 'Use o <nowrap>e-mail</nowrap> e a senha que você criou ao ativar a conta.',
    email: 'E-mail',
    password: 'Senha',
    submit: 'Entrar',
    submitting: 'Entrando...',
    noAccount: 'Ainda não tem conta?',
    createAccount: 'Criar conta',
  },
  welcome: {
    title: 'Olá, {{name}}',
    intro: 'Seu saldo e suas movimentações vão aparecer aqui.',
  },
  inbox: {
    title: 'Caixa de entrada demo',
    intro:
      'E-mails enviados pelo serviço de notificação simulado. Só existe no ambiente de demonstração.',
    email: 'E-mail',
    submit: 'Ver e-mails',
    submitting: 'Buscando...',
    empty: 'Nenhum e-mail para {{recipient}}.',
    listLabel: 'Mensagens',
    openLink: 'Abrir link',
  },
  validation: {
    nameRequired: 'Informe o nome completo',
    nameTooLong: 'Use no máximo 100 caracteres',
    emailRequired: 'Informe o e-mail',
    emailTooLong: 'E-mail longo demais',
    emailInvalid: 'E-mail inválido',
    cpfInvalid: 'CPF inválido',
    passwordRequired: 'Informe a senha',
    passwordTooShort: 'Use pelo menos 8 caracteres',
    passwordTooLong: 'Use no máximo 72 caracteres',
    passwordTooManyBytes: 'Senha longa demais',
    passwordConfirmationRequired: 'Repita a senha',
    passwordMismatch: 'As senhas não são iguais',
  },
  /** Messages per Problem Details `type`; any other type shows `fallback`. */
  problem: {
    'validation-error': 'Confira os dados informados.',
    'invalid-cursor': 'Não foi possível carregar mais itens. Recarregue a página.',
    'invalid-token': 'Link inválido ou expirado.',
    unauthorized: 'Sua sessão expirou. Entre novamente.',
    'invalid-credentials': 'E-mail ou senha inválidos.',
    'invalid-refresh-token': 'Sua sessão expirou. Entre novamente.',
    forbidden: 'Você não tem permissão para acessar isto.',
    'account-blocked': 'Sua conta está bloqueada para movimentações.',
    'not-found': 'Não encontrado.',
    'recipient-not-found': 'Destinatário não encontrado ou indisponível.',
    'concurrency-conflict': 'Muitas operações ao mesmo tempo. Tente novamente.',
    'insufficient-balance': 'Saldo insuficiente.',
    'daily-limit-exceeded': 'Valor acima do limite diário disponível.',
    'self-transfer': 'Não é possível transferir para a própria conta.',
    'recipient-unavailable': 'Destinatário indisponível. Busque novamente.',
    'network-error': 'Sem conexão com o servidor. Verifique sua internet e tente novamente.',
    fallback: 'Algo deu errado. Tente novamente em instantes.',
  },
}

export type Catalog = typeof ptBR
