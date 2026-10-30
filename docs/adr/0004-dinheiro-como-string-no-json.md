# 4. Dinheiro como string no JSON

Data: 2026-10-30

## Status

Aceita

## Contexto

O backend trata dinheiro como `BigDecimal` com escala 2 e o banco como `DECIMAL(19,2)`, como exige o PRD. O `number` do JSON, lido pelo JavaScript, é um ponto flutuante de 64 bits: valores como `0.1` não têm representação exata, e valores acima de 2^53 centavos perdem precisão. O exemplo de requisição do PRD usa número (`"amount": 150.00`).

## Decisão

- Respostas sempre escrevem valores monetários como string com duas casas decimais (`"150.00"`), pelo tipo `Money` e seu serializador.
- Requisições aceitam string ou número. O número é lido pelo texto original, sem passar por `double`, então `0.1` vira exatamente `0.10`.
- Mais de duas casas decimais é rejeitado com 400 `validation-error`, apontando o campo; nunca há arredondamento.
- O frontend mantém os valores como string e só os converte para exibição com `Intl.NumberFormat`. Contas de dinheiro não são feitas no cliente.

## Consequências

- O exemplo do PRD continua válido, porque número também é aceito.
- Clientes precisam tratar o valor como string, o que é explícito no contrato da API e no catálogo de convenções.
- Qualquer DTO que exponha dinheiro usa `Money`, e nunca `BigDecimal` ou `double` direto, para garantir o formato.
