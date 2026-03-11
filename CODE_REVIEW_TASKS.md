# Revisão rápida da base de código

## Problemas encontrados

1. **Erro de digitação em comentário (Java)**  
   No caminho de conversão reversa, o comentário diz `WAV->CAS->BIN->WAV`, mas o fluxo implementado termina em BASIC (`Bin2Bas`), então o correto é `WAV->CAS->BIN->BAS`.

2. **Bug de parsing da opção `-i` (Python)**  
   O parser tenta converter `sys.argv[num_arg]` para inteiro sem antes avançar para o valor da opção. Assim, ao usar `-i 2`, o código tenta converter a string `"-i"` e falha sempre.

3. **Discrepância de documentação/mensagem (Python)**  
   A mensagem de erro menciona suporte a extensão `TXT` (`BAS/TXT/BIN/CAS/WAV`), porém o regex e a lógica aceitam apenas `bas|bin|cas|wav`.

4. **Lacuna de teste de regressão (Projeto)**  
   Não há testes automatizados para argumentos de CLI e fluxos básicos de conversão. Isso facilita regressões em opções como `-i` e em mensagens/sintaxe de ajuda.

## Tarefas sugeridas

### 1) Corrigir erro de digitação
- **Título:** Ajustar comentário de conversão reversa em `src/MC1000CasTools.java`.
- **Objetivo:** Trocar `WAV` por `BAS` no comentário do bloco reverso.
- **Critério de aceite:** Comentário descreve exatamente o fluxo real do código (`WAV->CAS->BIN->BAS`).

### 2) Corrigir bug
- **Título:** Consertar parsing da opção `-i` em `py/MC1000CasTools.py`.
- **Objetivo:** Avançar o índice de argumento antes de converter o valor de `-i`.
- **Critério de aceite:** Comando `python3 py/MC1000CasTools.py -i 2 <arquivo>.wav -list` não falha por parsing de `-i`; só deve falhar por problemas reais de arquivo/formato.

### 3) Ajustar comentário/documentação
- **Título:** Alinhar mensagem de extensões suportadas na versão Python.
- **Objetivo:** Remover `TXT` da mensagem de erro, ou implementar suporte real a `.txt` (preferível ajustar a mensagem para refletir o comportamento atual).
- **Critério de aceite:** Mensagem de erro coincide com as extensões aceitas pela regex e parser.

### 4) Melhorar teste
- **Título:** Adicionar testes de regressão de CLI para Python.
- **Objetivo:** Criar testes automatizados para:
  - parsing de `-i` com valor válido e inválido;
  - validação de extensões de entrada;
  - geração/validação de mensagens de erro esperadas.
- **Critério de aceite:** Suíte reproduz o bug atual de `-i` (antes da correção) e passa após correção; cobertura mínima para caminhos de erro de argumentos.
