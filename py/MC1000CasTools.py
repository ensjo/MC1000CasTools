import io
import sys
import os
import re
import wave

PALAVRAS_RESERVADAS = [
    b"END", b"FOR", b"NEXT", b"DATA", b"EXIT", b"INPUT", b"DIM", b"READ",
    b"LET", b"GOTO", b"RUN", b"IF", b"RESTORE", b"GOSUB", b"RETURN", b"REM",
    b"STOP", b"OUT", b"ON", b"HOME", b"WAIT", b"DEF", b"POKE", b"PRINT",
    b"PR#", b"SOUND", b"GR", b"HGR", b"COLOR", b"TEXT", b"PLOT", b"TRO",
    b"UNPLOT", b"SET", b"CALL", b"DRAW", b"UNDRAW", b"TEMPO", b"WIDTH", b"CONT",
    b"LIST", b"CLEAR", b"LOAD", b"SAVE", b"NEW", b"TLOAD", b"COLUMN", b"AUTO",
    b"FAST", b"SLOW", b"EDIT", b"INVERSE", b"NORMAL", b"DEBUG", b"TAB(", b"TO",
    b"FN", b"SPC(", b"THEN", b"NOT", b"STEP", b"VTAB(", b"+", b"-",
    b"*", b"/", b"^", b"AND", b"OR", b">", b"=", b"<",
    b"SGN", b"INT", b"ABS", b"USR", b"FRE", b"INP", b"POS", b"SQR",
    b"RND", b"LOG", b"EXP", b"COS", b"SIN", b"TAN", b"ATN", b"PEEK",
    b"LEN", b"STR$", b"VAL", b"ASC", b"CHR$", b"LEFT$", b"RIGHT$", b"MID$"
]

FORMATO_INDEFINIDO = 0
FORMATO_BAS = 1
FORMATO_BIN = 2
FORMATO_CAS = 3
FORMATO_WAV = 4

e_programa_basic = False
nome_de_arquivo_em_cassete = b""
indice_de_arquivo_em_cassete_a_extrair = 1
indice_de_arquivo_em_cassete_a_extrair_informado = False
modo_verboso = False

def main():
    global e_programa_basic
    global nome_de_arquivo_em_cassete
    global indice_de_arquivo_em_cassete_a_extrair
    global indice_de_arquivo_em_cassete_a_extrair_informado
    global modo_verboso
    
    num_arg = 1
    opcao: str = None
    nome_do_arquivo_origem: str = None
    extensao_do_arquivo_origem: str = None
    arquivo_origem = None
    nome_do_arquivo_destino: str = None
    extensao_do_arquivo_destino: str = None
    formato_origem = FORMATO_INDEFINIDO
    formato_destino = FORMATO_INDEFINIDO
    usar_stdout = False
    sintaxe_incorreta = True

    # Trata opções de linha de comando.
    while num_arg < len(sys.argv):
        opcao = sys.argv[num_arg].lower()
        if opcao == "-b":
            e_programa_basic = True
            num_arg += 1
        elif opcao == "-n":
            num_arg += 1
            if num_arg < len(sys.argv):
                nome_de_arquivo_em_cassete = bytes(sys.argv[num_arg], "utf-8")
                num_arg += 1
        elif opcao == "-i":
            if num_arg < len(sys.argv):
                try:
                    indice_de_arquivo_em_cassete_a_extrair = int(sys.argv[num_arg])
                except ValueError:
                    print("Parâmetro inválido para a opção -i. Deve ser um número inteiro.", file=sys.stderr)
                    sys.exit(1)
                else:
                    indice_de_arquivo_em_cassete_a_extrair_informado = True
                num_arg += 1
        elif opcao == "-v":
            modo_verboso = True
            num_arg += 1
        else:
            break

    if num_arg < len(sys.argv):
        nome_do_arquivo_origem = sys.argv[num_arg]
        num_arg += 1
        if modo_verboso:
            print(f"Arquivo de origem: [{nome_do_arquivo_origem}].")
        try:
            arquivo_origem = open(nome_do_arquivo_origem, "rb")
        except FileNotFoundError:
            print(">>>! Arquivo não existe.", file=sys.stderr)
            sys.exit(1)
        if os.stat(nome_do_arquivo_origem).st_size == 0:
            print(">>>! Arquivo vazio.", file=sys.stderr)
            sys.exit(1)
        p = re.compile(r"^(.+?)(\.bas)?\.(bas|bin|cas|wav)$", re.IGNORECASE)
        m = p.search(nome_do_arquivo_origem)
        if m:
            extensao_do_arquivo_origem = m.group(3)
            if extensao_do_arquivo_origem.lower() == "bas":
                e_programa_basic = True
                formato_origem = FORMATO_BAS
            elif extensao_do_arquivo_origem.lower() == "bin":
                formato_origem = FORMATO_BIN
            elif extensao_do_arquivo_origem.lower() == "cas":
                formato_origem = FORMATO_CAS
            else:  # elif extensao_do_arquivo_origem.lower() == "wav":
                formato_origem = FORMATO_WAV
            if num_arg < len(sys.argv):
                opcao = sys.argv[num_arg].lower()
                if opcao == "-bas":
                    formato_destino = FORMATO_BAS
                elif opcao == "-bin":
                    formato_destino = FORMATO_BIN
                elif opcao == "-cas":
                    formato_destino = FORMATO_CAS
                elif opcao == "-wav":
                    formato_destino = FORMATO_WAV
                elif opcao == "-list":
                    formato_destino = FORMATO_BAS
                    usar_stdout = True
                if formato_destino != FORMATO_INDEFINIDO:
                    # Foi informada uma opção de formato de destino.
                    # Calcular nome do arquivo de destino a partir do nome do arquivo de origem.
                    nome_do_arquivo_destino = m.group(1) + \
                        (m.group(2) if formato_destino != FORMATO_BAS and m.group(2) is not None else "") + \
                        (f".{m.group(3)}" if formato_origem == FORMATO_BAS else "") + \
                        (f"({indice_de_arquivo_em_cassete_a_extrair})" if indice_de_arquivo_em_cassete_a_extrair_informado else "") + \
                        "." + \
                        ("bas" if formato_destino == FORMATO_BAS else \
                         "bin" if formato_destino == FORMATO_BIN else \
                         "cas" if formato_destino == FORMATO_CAS else \
                         "wav")
                else:
                    # Não foi informada uma opção de formato de destino.
                    # Obter o nome do arquivo de destino da linha de comando.
                    nome_do_arquivo_destino = sys.argv[num_arg]
                    m = p.search(nome_do_arquivo_destino)
                    if m:
                        extensao_do_arquivo_destino = m.group(3)
                        if extensao_do_arquivo_destino.lower() == "bas":
                            formato_destino = FORMATO_BAS
                        elif extensao_do_arquivo_destino.lower() == "bin":
                            formato_destino = FORMATO_BIN
                        elif extensao_do_arquivo_destino.lower() == "cas":
                            formato_destino = FORMATO_CAS
                        else:  # elif extensao_do_arquivo_destino.lower() == "wav":
                            formato_destino = FORMATO_WAV
                    else:
                        print(">>>! Extensão do arquivo de destino não reconhecida, deve ser BAS/BIN/CAS/WAV.", file=sys.stderr)
                        sys.exit(1)
                if formato_destino == formato_origem:
                    print(">>>! Formato de destino igual ao formato de origem.", file=sys.stderr)
                    sys.exit(1)
                sintaxe_incorreta = False
        else:
            print(">>>! Extensão do arquivo de origem não reconhecida, deve ser BAS/TXT/BIN/CAS/WAV.", file=sys.stderr)
            sys.exit(1)
    if sintaxe_incorreta:
        print("""Uso:

   MC1000CasTools.py <opções> <arq_origem> -<formato>
   MC1000CasTools.py <opções> <arq_origem> -list
   MC1000CasTools.py <opções> <arq_origem> <arq_destino>

Converte arquivos entre formatos diferentes:

   *.bas : Arquivo contendo código-fonte de programa BASIC do MC1000.
   *.bin : Arquivo contendo dados brutos de um bloco de memória (no caso de um
        programa BASIC, é o programa em formato tokenizado).
   *.cas : Arquivo contendo dados tal como gerados/lidos pelos comandos SAVE,
        LOAD e TLOAD do MC1000: cabeçalho (nome de arquivo em cassete,
        endereço de início, endereço de fim) + dados brutos.
   *.wav : Som produzido pelo MC1000 correspondente aos dados de cassete.

Opções:

   -b : Na conversão de BIN para CAS ou WAV, indica que o conteúdo do arquivo
        é um programa BASIC, para que o nome de arquivo em cassete seja
        adequadamente formatado (até 5 caracteres, completados com espaços).
   -n <nome> : Na conversão de BAS ou BIN para CAS ou WAV, especifica o nome de
        arquivo em cassete (até 14 caracteres). O valor predefinido é vazio.
   -i <número> : Se um arquivo WAV contém mais de um arquivo de cassete, indica
        qual deles converter. O valor predefinido é 1.
   -v : Modo verboso. Exibe diversas informações sobre o processo de conversão.

Outros parâmetros:

   <arq_origem> : O arquivo a ser convertido. O formato será reconhecido pela
        extensão.
   -<formato> : O formato final da conversão: -bas, -bin, -cas ou -wav.
        Se esta opção for usada, o nome do arquivo de destino será o mesmo do
        arquivo de origem com a extensão devidamente modificada.
   -list : Converte para formato BASIC e exibe na tela, sem gerar arquivo.
   <arq_destino> : Se não for especificado um formato de conversão, deve-se
        fornecer o nome do arquivo de destino. O formato da conversão será
        detectado pela extensão.

Nota: Esta ferramenta extrai apenas o primeiro arquivo de cassete contido em um
arquivo WAV. Se houver mais arquivos de cassete, o usuário deverá dividir o
arquivo WAV em arquivos menores por conta própria.

Para maiores informações visite:
https://sites.google.com/site/ccemc1000/sistema/cassete""", file=sys.stderr)
        sys.exit(1)

    if modo_verboso and not usar_stdout:
        print(f"Arquivo de destino: [{nome_do_arquivo_destino}].")

    # Conecta os componentes da conversão.
    entrada = arquivo_origem
    if formato_origem < formato_destino:
        # Conversão no sentido BAS->BIN->CAS->WAV.
        if formato_origem == FORMATO_BAS:
            entrada = Bas2Bin(entrada)
        if formato_origem <= FORMATO_BIN and formato_destino >= FORMATO_CAS:
            entrada = Bin2Cas(entrada)
        if formato_destino == FORMATO_WAV:
            entrada = Cas2Wav(entrada)
    else:
        # Conversão no sentido WAV->CAS->BIN->BAS.
        if formato_origem == FORMATO_WAV:
            entrada = Wav2Cas(entrada)
        if formato_origem >= FORMATO_CAS and formato_destino <= FORMATO_BIN:
            entrada = Cas2Bin(entrada)
        if formato_destino == FORMATO_BAS:
            entrada = Bin2Bas(entrada)
    saida = sys.stdout if usar_stdout else open(nome_do_arquivo_destino, "wb")
    
    try:
        # Lê bytes para o arquivo de saída.
        while True:
            byte = entrada.read(1)
            if byte:
                if saida is sys.stdout:
                    saida.write(byte.decode("latin_1"))
                else:
                    saida.write(byte)
            else:
                break
    finally:
        # Fecha e termina.
        entrada.close()
        saida.close()


class MyIOBase(io.IOBase):
    def close(self):
        self.entrada.close()
        super().close()

    def read(self, size=-1) -> bytes:
        retorno = bytearray()
        while size == -1 or len(retorno) < size:
            byte: int = self.read_1_byte()
            if byte is not None:
                retorno.append(byte)
            else:
                break
        return bytes(retorno)


# CONVERSÃO BASIC --> BINÁRIO.

class Bas2Bin(MyIOBase):
    ESTRATEGIA_TOKEN = 0
    ESTRATEGIA_DATA = 1
    ESTRATEGIA_DATA2 = 2
    ESTRATEGIA_REM = 3
    ESTRATEGIA_STRING_OU_CARACTER = 4
    ESTRATEGIA_STRING = 5
    ESTRATEGIA_CARACTER = 6
    
    p_linha_com_numero = re.compile(br"^\s*(\d+)\s*(.*)")
    
    def __init__(self, entrada: io.IOBase):
        self.entrada = entrada
        self.fim_de_dados = False
        self.linha_bin: bytearray = None
        self.endereco_da_proxima_linha = 0x03d5  # Endereço padrão do início do programa BASIC na memória.
    
    def read_1_byte(self) -> int:
        # Enquanto houver bytes no buffer de saída, fornece-os.
        if self.linha_bin:
            return self.linha_bin.pop(0)
        # Retorna None se não houver mais dados de entrada.
        if self.fim_de_dados:
            return None
        # Procura uma linha de programa BASIC válida.
        while True:
            # Tenta ler uma linha.
            self.linha_bas = self.entrada.readline()
            if self.linha_bas == b"":
                # Não há mais linhas:
                # 2 bytes 0 indicam o fim do programa.
                # O comando SAVE salva um byte extra ao fim.
                self.fim_de_dados = True
                self.linha_bin.extend(b"\0\0\0")
                return self.read_1_byte()
            self.linha_bas = self.linha_bas.rstrip(b"\r\n")  # Remove "\r" e "\n" ao final da linha.
            if modo_verboso:
                print(f"Bas2Bin> {self.linha_bas.decode('latin_1')}")
            # Checa se a linha consiste de número de linha (0~65535) + conteúdo.
            m = self.p_linha_com_numero.search(self.linha_bas)
            if m:
                # Extrai o número da linha.
                numero_da_linha = int(m.group(1)) if len(m.group(1)) <= 5 else None
                if numero_da_linha is None or numero_da_linha > 65535:
                    # Descarta linha se número inválido.
                    print("Bas2Bin>! Linha ignorada: Número de linha ausente ou maior que 65535.", file=sys.stderr)
                else:
                    # Número válido. Converte o conteúdo para maiúsculas e conclui a busca.
                    self.linha_bas = bytearray(m.group(2).upper())
                    break
            else:
                print("Bas2Bin>! Linha ignorada: Número de linha ausente.", file=sys.stderr)

        # Buffer que conterá a linha tokenizada.
        # 2 bytes de endereço da próxima linha.
        # + 2 bytes de número de linha.
        # + conteúdo da linha
        # + 1 byte 0 ao fim da linha.
        self.linha_bin = bytearray()
        
        # Endereço da próxima linha (a definir mais tarde).
        self.linha_bin.extend((0x0000).to_bytes(2, byteorder="little"))
        
        # Número da linha.
        self.linha_bin.extend((numero_da_linha).to_bytes(2, byteorder="little"))
        
        # Começa tokenizando o conteúdo da linha.
        self.estrategia = self.ESTRATEGIA_TOKEN
        
        while self.linha_bas:
            self.caracter = bytes([self.linha_bas.pop(0)])
            self.exec(self.estrategia)
        
        # Byte 0 termina a linha.
        self.linha_bin.extend(b"\0")
        
        # Atualiza o endereço da próxima linha no início do buffer de saída.
        self.endereco_da_proxima_linha += len(self.linha_bin)
        self.linha_bin[:2] = (self.endereco_da_proxima_linha).to_bytes(2, byteorder="little")
        
        return self.read_1_byte()
        
    def exec(self, estrategia_exec):
        if estrategia_exec == self.ESTRATEGIA_TOKEN:  # Estratégia padrão: Descarta espaços e tokeniza palavras reservadas.
            if self.caracter == b" ":
                # Descarta espaços.
                pass
            else:
                # Verifica se encontrou uma palavra reservada.
                linha_bas_tmp = self.caracter + self.linha_bas  # Restaura o caracter extraído da linha.
                token = None
                for i in range(len(PALAVRAS_RESERVADAS)):
                    palavra_reservada = PALAVRAS_RESERVADAS[i]
                    if linha_bas_tmp.startswith(palavra_reservada):
                        token = i ^ 0x80
                        break
                if token is not None:
                    # Tokeniza.
                    self.linha_bin.append(token)
                    # Pula as letras da palavra reservada.
                    del self.linha_bas[:len(palavra_reservada)]
                    # Certas palavras reservadas implicam mudança de estratégia.
                    if palavra_reservada == b"DATA":
                        self.estrategia = self.ESTRATEGIA_DATA
                    elif palavra_reservada in [b"REM", b"SAVE", b"LOAD"]:
                        self.estrategia = self.ESTRATEGIA_REM
                else:
                    # Outros caracteres.
                    self.exec(self.ESTRATEGIA_STRING_OU_CARACTER)
        elif estrategia_exec == self.ESTRATEGIA_DATA:  # Trata o início de uma instrução DATA: Descarta espaços iniciais não escapados, se houver.
            if self.caracter == b" ":
                # Descarta espaços iniciais não escapados após a palavra "DATA".
                pass
            else:
                # Espaços escapados ou quaisquer outros caracteres marcam o início dos dados reais.
                self.estrategia = self.ESTRATEGIA_DATA2
                self.exec(self.ESTRATEGIA_DATA2)
        elif estrategia_exec == self.ESTRATEGIA_DATA2:  # Trata caracteres entre dados em uma instrução DATA: espaços não escapados após o primeiro caracter diferente de espaço saem não escapados.
            self.exec(self.ESTRATEGIA_STRING_OU_CARACTER)
            if self.caracter == b":":
                # Fim da instrução DATA.
                self.estrategia = self.ESTRATEGIA_TOKEN
        elif estrategia_exec == self.ESTRATEGIA_REM:  # Trata o início de uma instrução REM/SAVE/LOAD: Descarta espaços iniciais não escapados, se houver.
            if self.caracter == b" ":
                # Descarta espaços iniciais não escapados após a palavra "REM".
                pass
            else:
                self.estrategia = self.ESTRATEGIA_CARACTER
                self.exec(self.ESTRATEGIA_CARACTER)
        elif estrategia_exec == self.ESTRATEGIA_STRING_OU_CARACTER:  # Identifica início de string.
            self.exec(self.ESTRATEGIA_CARACTER)
            if self.caracter == b'"':
                # Início de string.
                # Salva estratégia atual para voltar ao final da string.
                self.estrategia_antes_de_string = self.estrategia
                self.estrategia = self.ESTRATEGIA_STRING
        elif estrategia_exec == self.ESTRATEGIA_STRING:  # Trata o corpo de uma string até encontrar aspas.
            self.exec(self.ESTRATEGIA_CARACTER)
            if self.caracter == b'"':
                # Fim da string.
                self.estrategia = self.estrategia_antes_de_string
        elif estrategia_exec == self.ESTRATEGIA_CARACTER:  # Trata caracteres.
            if self.caracter == b"~":
                # Notação hexadecimal ~XX.
                if re.search(br"^[0-9A-Fa-f]{2}", self.linha_bas):
                    self.linha_bin.append(int(self.linha_bas[:2], 16))
                    del self.linha_bas[:2]
                else:
                    self.linha_bin.extend(self.caracter)
            elif self.caracter == b"`":
                # Notação de caracter inverso: `X.
                if self.linha_bas:
                    self.linha_bin.append(self.linha_bas.pop(0) ^ 0x80)
                else:
                    self.linha_bin.extend(self.caracter)
            else:
                self.linha_bin.extend(self.caracter)


# CONVERSÃO BINÁRIO --> CASSETE.

class Bin2Cas(MyIOBase):
    def __init__(self, entrada: io.IOBase):
        self.entrada: io.IOBase = entrada
        self.dados: bytearray = None
        self.cabecalho: bytearray = None

    def read_1_byte(self) -> int:
        global e_programa_basic
        global nome_de_arquivo_em_cassete

        if self.dados is None:
            self.dados = bytearray()
            self.cabecalho = bytearray()

            # Carrega o bloco de dados.
            self.dados.extend(self.entrada.read())

            # Ajusta nome de arquivo em cassete.
            if e_programa_basic:
                # Se o arquivo é um programa BASIC, limita nome de arquivo em cassete a 5 caracteres, preenchidos com espaços.
                nome_de_arquivo_em_cassete = nome_de_arquivo_em_cassete.upper()[:5].ljust(5)

            if len(nome_de_arquivo_em_cassete) < 14:
                # Acrescenta CR ao fim quando nome de arquivo em cassete tem menos de 14 caracteres.
                nome_de_arquivo_em_cassete += b"\r"
            elif nome_de_arquivo_em_cassete > 14:
                nome_de_arquivo_em_cassete = nome_de_arquivo_em_cassete[:14]

            # Compõe cabecalho:
            # 1. Nome de arquivo em cassete:
            self.cabecalho.extend(nome_de_arquivo_em_cassete)
            # 2. Endereço inicial do bloco de dados (0x0000):
            self.cabecalho.extend((0x0000).to_bytes(2, byteorder="little"))
            # 3. Endereço final do bloco de dados (= tamanho do bloco):
            self.cabecalho.extend(len(self.dados).to_bytes(2, byteorder="little"))

            return self.read_1_byte()
        elif self.cabecalho:
            return self.cabecalho.pop(0)
        elif self.dados:
            return self.dados.pop(0)
        else:
            return None


# CONVERSÃO CASSETE --> WAV.

class Cas2Wav(MyIOBase):
    def __init__(self, entrada: io.IOBase):
        self.entrada = entrada
        self.dados: io.BytesIO = None
        self.wav: wave.Wave_write = None

    def read_1_byte(self) -> int:
        if self.dados is None:
            # Inicializa e parametriza arquivo WAV em memória.
            self.dados = io.BytesIO()
            self.wav = wave.open(self.dados, "wb")
            self.wav.setnchannels(1)  # Mono.
            self.wav.setsampwidth(1)  # 1 byte por amostra.
            self.wav.setframerate(11025)

            # Gera sinal piloto:
            # 4096 períodos curtos.
            for i in range(4096):
                self.periodo_curto()
            # 256 períodos longos.
            for i in range(256):
                self.periodo_longo()

            # Converte os bytes em sinais sonoros.
            while True:
                byte = self.entrada.read(1)
                if byte:
                    self.grava_byte(ord(byte))
                else:
                    break

            # Algumas amostras baixas no final para fazer contraste
            # com as amostras altas no final dos dados.
            self.amostras_baixas()
            self.amostras_baixas()
            self.amostras_baixas()
            self.amostras_baixas()

            self.wav.close()  # Encerra objeto wave.
            self.dados.seek(0)  # Recua ao início dos dados brutos.

        byte = self.dados.read(1)
        if byte:
            return ord(byte)
        else:
            return None

    def grava_byte(self, byte: int):
        # Marca de início de byte.
        self.periodo_curto()
        # Os oito bits do byte, do menos para o mais significativo.
        par = True
        for i in range(8):
            if byte & (1 << i):
                # Bit 1.
                self.periodo_curto()
                par = not par
            else:
                # Bit 0.
                self.periodo_longo()
        # Bit de paridade.
        if par:
            self.periodo_curto()
        else:
            self.periodo_longo()

    def periodo_curto(self):
        self.amostras_baixas()
        self.amostras_altas()

    def periodo_longo(self):
        self.amostras_baixas()
        self.amostras_baixas()
        self.amostras_altas()
        self.amostras_altas()

    def amostras_baixas(self):
        # 4 amostras a 11025 amostras por segundo.
        self.wav.writeframes(bytes([0, 0, 0, 0]))


    def amostras_altas(self):
        # 4 amostras a 11025 amostras por segundo.
        self.wav.writeframes(bytes([255, 255, 255, 255]))


# CONVERSÃO WAV --> CASSETE.

class Wav2Cas(MyIOBase):
    PROCURANDO_64_PULSOS_CURTOS = 0
    LENDO_64_PULSOS_CURTOS = 1
    PROCURANDO_64_PULSOS_LONGOS = 2
    LENDO_64_PULSOS_LONGOS = 3
    PROCURANDO_INICIO_DO_BYTE = 4
    LENDO_INICIO_DO_BYTE = 5
    LENDO_BYTE = 6
    LENDO_PARIDADE = 7

    # Aqui o termo "pulso" refere-se a uma sequência de amostras altas.
    # No MC-1000, um pulso curto representa o bit 1, um pulso longo
    # representa o bit 0.
    # Um pulso curto ocupa cerca de 16 amostras em um arquivo
    # com 44100 amostras por segundo, aprox. 3.628118E-4 segundos.
    DURACAO_DO_PULSO_CURTO = 16 / 44100
    # Um pulso longo dura o dobro do tempo.
    # Limites das faixas usadas para reconhecer pulsos curtos,
    # pulsos longos, e ruído (pulsos curtos demais ou longos demais).
    DURACAO_MIN_DO_PULSO_CURTO = DURACAO_DO_PULSO_CURTO * pow(2, -1.0)
    DURACAO_MAX_DO_PULSO_CURTO = DURACAO_DO_PULSO_CURTO * pow(2, +0.5)
    DURACAO_MAX_DO_PULSO_LONGO = DURACAO_DO_PULSO_CURTO * pow(2, +2.0)

    PULSO_CURTO_DEMAIS = 2
    PULSO_CURTO_BIT_1 = 1  # = Bit 1.
    PULSO_LONGO_BIT_0 = 0  # = Bit 0.
    PULSO_LONGO_DEMAIS = -1

    PROCURANDO_ARQUIVO = 0
    LENDO_NOME_DO_ARQUIVO = 1
    LENDO_ENDERECO_DE_INICIO = 2
    LENDO_ENDERECO_DE_FIM = 3
    LENDO_BLOCO_DE_DADOS = 4

    def __init__(self, entrada: io.IOBase):
        self.entrada = entrada
        self.duracao_da_amostra: float
        self.amostras: bytearray = None
        self.indice_amostras: int
        self.amostra_max: int
        self.amostra_min: int
        self.fator_de_amplitude_das_amostras: float
        self.wav: wave.Wave_read

        # Aqui o termo "pulso" refere-se a uma sequência de amostras altas.
        self.duracao_do_pulso: float

        self.estado: int
        self.amostra: int
        self.sinal_da_amostra: int
        self.valor_do_pulso: int
        self.contador_de_pulsos: int
        self.byte: int
        self.par: bool
        self.fim_de_byte = False
        self.indice_de_arquivo_em_cassete = 0
        self.extrair_este_arquivo = False
        self.fim_de_arquivo = False

        self.estado2: int
        self.nome_de_arquivo_em_cassete: bytearray
        self.endereco_de_inicio: int
        self.endereco_de_fim: int
        self.tamanho_do_bloco_de_dados: int
        self.contador_de_bytes: int

    def read_1_byte(self):
        if self.amostras is None:
            self.wav = wave.open(self.entrada, "rb")
            if modo_verboso:
                print(f"Wav2Cas> Quantidade de canais: {self.wav.getnchannels()}.")
                print(f"Wav2Cas> Largura da amostra: {self.wav.getsampwidth()} byte(s).")
                print(f"Wav2Cas> Taxa de quadros: {self.wav.getframerate()}.")
                print(f"Wav2Cas> Quantidade de quadros: {self.wav.getnframes()}.")
            self.amostras = bytearray(self.wav.getnframes())
            self.indice_amostras = 0
            self.amostra_max = 0
            self.amostra_min = 255
            while True:
                quadro = self.wav.readframes(1)
                if quadro:
                    if self.wav.getsampwidth() == 1:
                        self.amostra = ord(quadro)  # 1 byte já sem sinal.
                    else:
                        self.amostra = (quadro[self.wav.getsampwidth()] + 128) & 0xff  # O byte mais significativo do primeiro canal, tornado sem sinal.

                    self.amostras[self.indice_amostras] = self.amostra
                    self.indice_amostras += 1
                    # Identifica valores mínimo e máximo para normalizar posteriormente.
                    if self.amostra > self.amostra_max:
                        self.amostra_max = self.amostra
                    if self.amostra < self.amostra_min:
                        self.amostra_min = self.amostra
                else:
                    break
            if modo_verboso:
                print("Wav2Cas> Análise concluída.")
                #print(f"Wav2Cas> Quantidade de quadros: {self.wav.getnframes()}.")
                print(f"Wav2Cas> Amostras obtidas: {self.indice_amostras}.")
                print(f"Wav2Cas> Mínimo: {self.amostra_min}.")
                print(f"Wav2Cas> Máximo: {self.amostra_max}.")

            if self.indice_amostras == 0:
                return None

            self.fator_de_amplitude_das_amostras = 255 / (self.amostra_max - self.amostra_min)  # Para adiantar cálculo de normalização.

            self.duracao_da_amostra = 1 / self.wav.getframerate()

            self.indice_amostras = 0
            self.trata_inicio_das_amostras()

        if self.fim_de_arquivo:
            return None
        else:
            while self.indice_amostras < len(self.amostras):
                # Normaliza amostra para o intervalo -128 ~ +127:
                self.amostra = (self.amostras[self.indice_amostras] - self.amostra_min) * self.fator_de_amplitude_das_amostras - 128

                if modo_verboso:
                    print(f"Amostra {self.indice_amostras}:{' ' * int((self.amostra + 128) * 50 / 255)}# {self.amostra}")

                self.indice_amostras += 1
                self.trata_amostra()
                if self.fim_de_byte:
                    self.fim_de_byte = False
                    if self.extrair_este_arquivo:
                        return self.byte

            self.trata_fim_das_amostras()

        return None

    def trata_inicio_das_amostras(self):
        self.estado = self.PROCURANDO_64_PULSOS_CURTOS
        self.estado2 = self.PROCURANDO_ARQUIVO
        self.sinal_da_amostra = -1

    def trata_amostra(self):
        if self.sinal_da_amostra == -1:
            if self.amostra >= 0:
                self.inicia_pulso()
        else:
            if self.amostra >= 0:
                self.duracao_do_pulso += self.duracao_da_amostra
            else:
                self.emite_pulso()

    def trata_fim_das_amostras(self):
        if self.sinal_da_amostra == +1:
            self.emite_pulso()
        self.trata_fim_dos_pulsos()

    def inicia_pulso(self):
        self.sinal_da_amostra = +1
        self.duracao_do_pulso = self.duracao_da_amostra

    def emite_pulso(self):
        if self.duracao_do_pulso < self.DURACAO_MIN_DO_PULSO_CURTO:
            self.valor_do_pulso = self.PULSO_CURTO_DEMAIS
        elif self.duracao_do_pulso < self.DURACAO_MAX_DO_PULSO_CURTO:
            self.valor_do_pulso = self.PULSO_CURTO_BIT_1
        elif self.duracao_do_pulso < self.DURACAO_MAX_DO_PULSO_LONGO:
            self.valor_do_pulso = self.PULSO_LONGO_BIT_0
        else:
            self.valor_do_pulso = self.PULSO_LONGO_DEMAIS
        self.trata_pulso()
        self.sinal_da_amostra = -1

    def trata_pulso(self):
        if self.estado == self.PROCURANDO_64_PULSOS_CURTOS:
            if self.valor_do_pulso == self.PULSO_CURTO_BIT_1:
                self.contador_de_pulsos = 1
                self.estado = self.LENDO_64_PULSOS_CURTOS
        elif self.estado == self.LENDO_64_PULSOS_CURTOS:
            if self.valor_do_pulso == self.PULSO_CURTO_BIT_1:
                self.contador_de_pulsos += 1
                if self.contador_de_pulsos == 64:
                    if modo_verboso:
                        print("Wav2Cas> Tom piloto, parte 1/2: Lidos 64 pulsos curtos.")
                    self.estado = self.PROCURANDO_64_PULSOS_LONGOS
            else:
                # Pulso estranho na sequência.
                # Reinicia a contagem.
                self.estado = self.PROCURANDO_64_PULSOS_CURTOS
        elif self.estado == self.PROCURANDO_64_PULSOS_LONGOS:
            if self.valor_do_pulso == self.PULSO_LONGO_BIT_0:
                self.contador_de_pulsos = 1
                self.estado = self.LENDO_64_PULSOS_LONGOS
        elif self.estado == self.LENDO_64_PULSOS_LONGOS:
            if self.valor_do_pulso == self.PULSO_LONGO_BIT_0:
                self.contador_de_pulsos += 1
                if self.contador_de_pulsos == 64:
                    if modo_verboso:
                        print("Wav2Cas> Tom piloto, parte 2/2: Lidos 64 pulsos longos.")
                    self.estado = self.PROCURANDO_INICIO_DO_BYTE
            else:
                # Pulso estranho na sequência.
                # Reinicia a contagem.
                self.estado = self.PROCURANDO_64_PULSOS_LONGOS
        elif self.estado == self.PROCURANDO_INICIO_DO_BYTE:
            if self.valor_do_pulso == self.PULSO_CURTO_BIT_1:
                self.trata_inicio_dos_bytes()
                self.inicia_byte()
        elif self.estado == self.LENDO_INICIO_DO_BYTE:
            if self.valor_do_pulso == self.PULSO_CURTO_DEMAIS:
                print("Wav2Cas>! Pulso de início de byte curto demais!", file=sys.stderr)
            elif self.valor_do_pulso in [self.PULSO_LONGO_BIT_0, self.PULSO_LONGO_DEMAIS]:
                print("Wav2Cas>! Pulso de início de byte longo demais!", file=sys.stderr)
            self.inicia_byte()
        elif self.estado == self.LENDO_BYTE:
            if self.valor_do_pulso in [self.PULSO_LONGO_BIT_0, self.PULSO_LONGO_DEMAIS]:
                if self.valor_do_pulso == self.PULSO_LONGO_DEMAIS:
                    print("Wav2Cas>! Pulso de bit longo demais!", file=sys.stderr)
            else:  # if self.valor_do_pulso in [self.PULSO_CURTO_BIT_1, self.PULSO_CURTO_DEMAIS]:
                if self.valor_do_pulso == self.PULSO_CURTO_DEMAIS:
                    print("Wav2Cas>! Pulso de bit curto demais!", file=sys.stderr)
                self.byte |= (1 << self.contador_de_pulsos)
                self.par = not self.par
            self.contador_de_pulsos += 1
            if self.contador_de_pulsos == 8:
                self.estado = self.LENDO_PARIDADE
        elif self.estado == self.LENDO_PARIDADE:
            if self.valor_do_pulso in [self.PULSO_LONGO_BIT_0, self.PULSO_LONGO_DEMAIS]:
                if self.valor_do_pulso == self.PULSO_LONGO_DEMAIS:
                    print("Wav2Cas>! Pulso de bit de paridade longo demais!", file=sys.stderr)
                if self.par:
                    print("Wav2Cas>! Erro de paridade.", file=sys.stderr)
            else:  # if self.valor_do_pulso in [self.PULSO_CURTO_BIT_1, self.PULSO_CURTO_DEMAIS]:
                if self.valor_do_pulso == self.PULSO_CURTO_DEMAIS:
                    print("Wav2Cas>! Pulso de bit de paridade curto demais!", file=sys.stderr)
            self.emite_byte()

    def trata_fim_dos_pulsos(self):
        if self.estado in [self.LENDO_BYTE, self.LENDO_PARIDADE]:
            self.emite_byte()
        self.trata_fim_dos_bytes()

    def trata_inicio_dos_bytes(self):
        pass

    def inicia_byte(self):
        self.estado = self.LENDO_BYTE
        self.contador_de_pulsos = 0
        self.byte = 0
        self.par = True

    def emite_byte(self):
        self.fim_de_byte = True
        if modo_verboso:
            print(f"Wav2Cas> Byte: {self.byte:02X} = {self.byte} = {self.byte if self.byte >= ord(b' ') else ord(b' '):c}")
        self.trata_byte()
        if self.estado2 == self.PROCURANDO_ARQUIVO:
            self.estado = self.PROCURANDO_64_PULSOS_CURTOS
        else:
            self.estado = self.LENDO_INICIO_DO_BYTE

    def trata_fim_dos_bytes(self):
        if self.estado2 == self.LENDO_BLOCO_DE_DADOS:
            self.emite_arquivo()

    def inicia_arquivo(self):
        global indice_de_arquivo_em_cassete_a_extrair

        self.indice_de_arquivo_em_cassete += 1
        self.extrair_este_arquivo = (self.indice_de_arquivo_em_cassete == indice_de_arquivo_em_cassete_a_extrair)
        self.nome_de_arquivo_em_cassete = bytearray()
        self.endereco_de_inicio = 0x0000
        self.endereco_de_fim = 0x0000
        self.tamanho_do_bloco_de_dados = 0
        self.estado2 = self.LENDO_NOME_DO_ARQUIVO
        self.contador_de_bytes = 0
        self.trata_byte()

    def trata_byte(self):
        global nome_de_arquivo_em_cassete

        if self.estado2 == self.PROCURANDO_ARQUIVO:
            self.inicia_arquivo()
        elif self.estado2 == self.LENDO_NOME_DO_ARQUIVO:
            if self.byte != ord(b"\r"):
                self.nome_de_arquivo_em_cassete.append(self.byte)
            if self.byte == ord(b"\r") or len(self.nome_de_arquivo_em_cassete) == 14:
                if modo_verboso:
                    print(f"Wav2Cas> Nome do arquivo em cassete: [{nome_de_arquivo_em_cassete.decode('latin_1')}]")
                nome_de_arquivo_em_cassete = bytes(self.nome_de_arquivo_em_cassete)
                self.estado2 = self.LENDO_ENDERECO_DE_INICIO
                self.contador_de_bytes = 0
        elif self.estado2 == self.LENDO_ENDERECO_DE_INICIO:
            self.contador_de_bytes += 1
            if self.contador_de_bytes == 1:
                self.endereco_de_inicio = self.byte  # LSB.
            else:  # if self.contador_de_bytes == 2:
                self.endereco_de_inicio |= (self.byte << 8)
                if modo_verboso:
                    print(f"Wav2Cas> Endereço de início: {self.endereco_de_inicio:04X} = {self.endereco_de_inicio}.")
                self.estado2 = self.LENDO_ENDERECO_DE_FIM
                self.contador_de_bytes = 0
        elif self.estado2 == self.LENDO_ENDERECO_DE_FIM:
            self.contador_de_bytes += 1
            if self.contador_de_bytes == 1:
                self.endereco_de_fim = self.byte  # LSB.
            else:  # if self.contador_de_bytes == 2:
                self.endereco_de_fim |= (self.byte << 8)
                if modo_verboso:
                    print(f"Wav2Cas> Endereço de fim: {self.endereco_de_fim:04X} = {self.endereco_de_fim}.")
                self.estado2 = self.LENDO_BLOCO_DE_DADOS
                self.contador_de_bytes = 0
        elif self.estado2 == self.LENDO_BLOCO_DE_DADOS:
            self.contador_de_bytes += 1
            if self.contador_de_bytes == self.tamanho_do_bloco_de_dados:
                self.emite_arquivo()

    def emite_arquivo(self):
        self.fim_de_arquivo = True
        if modo_verboso:
            print("Wav2Cas> Fim de arquivo em cassete.")
        self.estado2 = self.PROCURANDO_ARQUIVO
        self.contador_de_bytes = 0


# CONVERSÃO CASSETE --> BINÁRIO.

class Cas2Bin(MyIOBase):
    LENDO_NOME_DO_ARQUIVO = 0
    LENDO_ENDERECO_DE_INICIO = 1
    LENDO_ENDERECO_DE_FIM = 2
    LENDO_BLOCO_DE_DADOS = 3

    def __init__(self, entrada: io.IOBase):
        self.entrada = entrada
        self.estado = self.LENDO_NOME_DO_ARQUIVO

    def read_1_byte(self) -> int:
        global nome_de_arquivo_em_cassete

        self.nome_de_arquivo_em_cassete = bytearray()
        self.endereco_de_inicio = 0x0000
        self.endereco_de_fim = 0x0000
        self.contador_de_bytes = 0
        while True:
            c = self.entrada.read(1)
            byte = ord(c) if c else None
            if c and self.estado != self.LENDO_BLOCO_DE_DADOS:
                if self.estado == self.LENDO_NOME_DO_ARQUIVO:
                    if c != b"\r":
                        self.nome_de_arquivo_em_cassete.append(byte)
                    if c == b"\r" or len(self.nome_de_arquivo_em_cassete) == 14:
                        if modo_verboso:
                            print(f"Cas2Bin> Nome do arquivo em cassete: [{self.nome_de_arquivo_em_cassete.decode('latin_1')}].")
                        nome_de_arquivo_em_cassete = bytes(self.nome_de_arquivo_em_cassete)
                        self.estado = self.LENDO_ENDERECO_DE_INICIO
                        self.contador_de_bytes = 0
                elif self.estado == self.LENDO_ENDERECO_DE_INICIO:
                    self.contador_de_bytes += 1
                    if self.contador_de_bytes == 1:
                        self.endereco_de_inicio = byte  # LSB.
                    else:  # if self.contador_de_bytes == 2:
                        self.endereco_de_inicio |= (byte << 8)  # MSB.
                        if modo_verboso:
                            print(f"Cas2Bin> Endereço de início: {self.endereco_de_inicio:04X} = {self.endereco_de_inicio}.")
                        self.estado = self.LENDO_ENDERECO_DE_FIM
                        self.contador_de_bytes = 0
                elif self.LENDO_ENDERECO_DE_FIM:
                    self.contador_de_bytes += 1
                    if self.contador_de_bytes == 1:
                        self.endereco_de_fim = byte  # LSB.
                    else:  # if self.contador_de_bytes == 2:
                        self.endereco_de_fim |= (byte << 8)  # MSB.
                        if modo_verboso:
                            print(f"Cas2Bin> Endereço de fim: {self.endereco_de_fim:04X} = {self.endereco_de_fim}.")
                        self.estado = self.LENDO_BLOCO_DE_DADOS
                        self.contador_de_bytes = 0
            else:
                break
        return byte


# CONVERSÃO BINÁRIO --> BASIC.

class Bin2Bas(MyIOBase):
    ESTRATEGIA_TOKEN = 0
    ESTRATEGIA_DATA = 1
    ESTRATEGIA_DATA2 = 2
    ESTRATEGIA_REM = 3
    ESTRATEGIA_STRING_OU_CARACTER = 4
    ESTRATEGIA_STRING = 5
    ESTRATEGIA_CARACTER = 6

    def __init__(self, entrada: io.IOBase):
        self.entrada = entrada
        self.linha_bas: bytearray = None
        self.fim_de_dados = False

    def read_1_byte(self) -> int:
        # Enquanto houver bytes no buffer de saída, fornece-os.
        if self.linha_bas:
            return self.linha_bas.pop(0)
        # Retorna None se não houver mais dados de entrada.
        if self.fim_de_dados:
            return None
        # Procura uma linha de programa BASIC válida.
        self.linha_bas = bytearray()

        c = self.entrada.read(2)  # Endereço da próxima linha.
        if len(c) == 2:
            if int.from_bytes(c, byteorder="little") == 0:
                # Fim do programa.
                return None
        else:
            print("Bin2Bas>! Fim de arquivo inesperado.", file=sys.stderr)
            return None

        c = self.entrada.read(2)  # Número da linha.
        if len(c) == 2:
            self.linha_bas.extend(bytes(str(int.from_bytes(c, byteorder="little")), "ascii"))
            self.linha_bas.extend(b" ")
        else:
            print("Bin2Bas>! Fim de arquivo inesperado.", file=sys.stderr)
            return None

        self.estrategia = self.ESTRATEGIA_TOKEN

        while True:
            c = self.entrada.read(1)
            if c:
                self.caracter = c
                if c == b"\0":
                    # Fim de linha.
                    break
                self.exec(self.estrategia)
            else:
                print("Bin2Bas>! Fim de arquivo inesperado.", file=sys.stderr)
                self.fim_de_dados = True
                break
        if modo_verboso:
            print(f"Bin2Bas> {self.linha_bas.decode('latin_1')}")
        self.linha_bas.extend(b"\n")
        return self.read_1_byte()

    def exec(self, estrategia_exec: int):
        if estrategia_exec == self.ESTRATEGIA_TOKEN:  # Estratégia padrão.
            if ord(self.caracter) & 0x80 and (ord(self.caracter) & 0x7f) < len(PALAVRAS_RESERVADAS):
                # Se encontrou token, exibe palavra reservada correspondente com espaços antes e depois.
                palavra_reservada = PALAVRAS_RESERVADAS[ord(self.caracter) & 0x7f]
                self.linha_bas.extend(b" ")
                self.linha_bas.extend(PALAVRAS_RESERVADAS[ord(self.caracter) & 0x7f])
                self.linha_bas.extend(b" ")
                # Certas palavras reservadas implicam mudança de estrategia.
                if palavra_reservada == b"DATA":
                    self.estrategia = self.ESTRATEGIA_DATA
                elif palavra_reservada in [b"REM", b"SAVE", b"LOAD"]:
                    self.estrategia = self.ESTRATEGIA_REM
            else:
                if self.caracter == b" ":
                    # Espaço: Introduz notação hexadecimal para que não sejam perdidos no Bas2Bin.
                    self.linha_bas.extend(b"~20")
                else:
                    self.exec(self.ESTRATEGIA_STRING_OU_CARACTER)
        elif estrategia_exec == self.ESTRATEGIA_DATA:  # Trata início de instrução DATA: espaços iniciais.
            if self.caracter == b" ":
                # Espaço: Exibe notação hexadecimal para marcar início dos dados reais.
                self.linha_bas.extend(b"~20")
                self.estrategia = self.ESTRATEGIA_DATA2
            else:
                self.estrategia = self.ESTRATEGIA_DATA2
        elif estrategia_exec == self.ESTRATEGIA_DATA2:  # Trata dado em instrução DATA.
            self.exec(self.ESTRATEGIA_STRING_OU_CARACTER)
            if self.caracter == b":":
                # Fim de instrução DATA.
                self.estrategia = self.ESTRATEGIA_TOKEN
        elif estrategia_exec == self.ESTRATEGIA_REM:  # Trata início de instrução REM/SAVE/LOAD: espaços iniciais.
            if self.caracter == b" ":
                # Espaço: Exibe notação hexadecimal para marcar início dos dados reais.
                self.linha_bas.extend(b"~20")
                self.estrategia = self.ESTRATEGIA_CARACTER  # Sai o resto da linha. Espaços após qualquer primeiro caracter saem não escapados.
            else:
                self.estrategia = self.ESTRATEGIA_CARACTER  # Sai o resto da linha. Espaços após qualquer primeiro caracter saem não escapados.
                self.exec(self.ESTRATEGIA_CARACTER)
        elif estrategia_exec == self.ESTRATEGIA_STRING_OU_CARACTER:  # Identifica início de string.
            self.exec(self.ESTRATEGIA_CARACTER)
            if self.caracter == b'"':
                # Início de string.
                # Salva estratégia atual para voltar ao final da string.
                self.estrategia_antes_de_string = self.estrategia
                self.estrategia = self.ESTRATEGIA_STRING
        elif estrategia_exec == self.ESTRATEGIA_STRING:  # Trata o corpo de uma string até encontrar aspas.
            self.exec(self.ESTRATEGIA_CARACTER)
            if self.caracter == b'"':
                # Fim da string.
                self.estrategia = self.estrategia_antes_de_string
        elif estrategia_exec == self.ESTRATEGIA_CARACTER:  # Trata caracteres.
            if ord(self.caracter) >= 32 and ord(self.caracter) < 96:
                # Caracter ASCII do MC6847.
                self.linha_bas.extend(self.caracter)
            elif (self.caracter ^ 0x80)  >= 32 and (self.caracter ^ 0x80) < 96:
                # Caracter ASCII do MC6847 inverso.
                # Listar como acento grave + caracter normal.
                self.linha_bas.extend(b"`")
                self.linha_bas.extend(self.caracter)
            else:
                # Outros caracteres.
                # Listar como til + código hexadecimal.
                self.linha_bas.extend(f"~{ord(self.caracter):02X}")


if __name__ == "__main__":
    main()
