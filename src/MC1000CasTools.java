import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class MC1000CasTools {

	final static String[] palavrasReservadas = new String[] {
		"END", "FOR", "NEXT", "DATA", "EXIT", "INPUT", "DIM", "READ",
		"LET", "GOTO", "RUN", "IF", "RESTORE", "GOSUB", "RETURN", "REM",
		"STOP", "OUT", "ON", "HOME", "WAIT", "DEF", "POKE", "PRINT",
		"PR#", "SOUND", "GR", "HGR", "COLOR", "TEXT", "PLOT", "TRO",
		"UNPLOT", "SET", "CALL", "DRAW", "UNDRAW", "TEMPO", "WIDTH", "CONT",
		"LIST", "CLEAR", "LOAD", "SAVE", "NEW", "TLOAD", "COLUMN", "AUTO",
		"FAST", "SLOW", "EDIT", "INVERSE", "NORMAL", "DEBUG", "TAB(", "TO",
		"FN", "SPC(", "THEN", "NOT", "STEP", "VTAB(", "+", "-",
		"*", "/", "^", "AND", "OR", ">", "=", "<",
		"SGN", "INT", "ABS", "USR", "FRE", "INP", "POS", "SQR",
		"RND", "LOG", "EXP", "COS", "SIN", "TAN", "ATN", "PEEK",
		"LEN", "STR$", "VAL", "ASC", "CHR$", "LEFT$", "RIGHT$", "MID$"
	};
	
	final static int FORMATO_INDEFINIDO = 0;
	final static int FORMATO_BAS = 1;
	final static int FORMATO_BIN = 2;
	final static int FORMATO_CAS = 3;
	final static int FORMATO_WAV = 4;
	
	private static boolean eProgramaBASIC = false;
	private static String nomeDeArquivoEmCassete = "";
	private static int indiceDeArquivoEmCasseteAExtrair = 1;
	private static boolean indiceDeArquivoEmCasseteAExtrairInformado = false;
	private static boolean modoVerboso = false;

	// MÉTODO MAIN.
	
	public static void main(String[] args)  throws FileNotFoundException, IOException {
		int numarg = 0;
		String opcao;
		String nomeDoArquivoOrigem;
		String extensaoDoArquivoOrigem;
		File arquivoOrigem = null;
		String nomeDoArquivoDestino = null;
		String extensaoDoArquivoDestino;
		int formatoOrigem = FORMATO_INDEFINIDO;
		int formatoDestino = FORMATO_INDEFINIDO;
		boolean usarStdout = false;
		boolean sintaxeIncorreta = true;
		
		// Trata opções de linha de comando.
		while (numarg < args.length) {
			if ((opcao = args[numarg]).equalsIgnoreCase("-b")) {
				eProgramaBASIC = true;
				numarg++;
			} else if (opcao.equalsIgnoreCase("-n")) {
				if (++numarg < args.length) {
					nomeDeArquivoEmCassete = args[numarg++];
				}
			} else if (opcao.equalsIgnoreCase("-i")) {
				if (++numarg < args.length) {
					try {
						indiceDeArquivoEmCasseteAExtrair = Integer.parseInt(args[numarg++]);
						indiceDeArquivoEmCasseteAExtrairInformado = true;
					} catch (NumberFormatException e) {
						System.err.println("Parâmetro inválido para a opção -i. Deve ser um número inteiro.");
					}
				}
			} else if (opcao.equalsIgnoreCase("-v")) {
				modoVerboso = true;
				numarg++;
			} else {
				break;
			}
		}
		if (numarg < args.length) {
			nomeDoArquivoOrigem = args[numarg++];
			if (modoVerboso) System.out.printf("Arquivo de origem: [%s].\n", nomeDoArquivoOrigem);
			arquivoOrigem = new File(nomeDoArquivoOrigem);
			if (!arquivoOrigem.exists()) {
				System.err.println(">>>! Arquivo não existe.");
				System.exit(0);
			}
			if (arquivoOrigem.length() == 0) {
				System.err.println(">>>! Arquivo vazio.");
				System.exit(0);
			}
			Pattern p = Pattern.compile("^(.+?)(\\.bas)?\\.(bas|bin|cas|wav)$", Pattern.CASE_INSENSITIVE);
			Matcher m = p.matcher(nomeDoArquivoOrigem);
			if (m.find()) {
				if ((extensaoDoArquivoOrigem = m.group(3)).equalsIgnoreCase("bas")) {
					eProgramaBASIC = true;
					formatoOrigem = FORMATO_BAS;
				} else if (extensaoDoArquivoOrigem.equalsIgnoreCase("bin")) {
					formatoOrigem = FORMATO_BIN;
				} else if (extensaoDoArquivoOrigem.equalsIgnoreCase("cas")) {
					formatoOrigem = FORMATO_CAS;
				} else /* if (extensaoDoArquivoOrigem.equalsIgnoreCase("wav")) */ {
					formatoOrigem = FORMATO_WAV;
				}
				if (numarg < args.length) {
					if ((opcao = args[numarg]).equalsIgnoreCase("-bas")) {
						formatoDestino = FORMATO_BAS;
					} else if (opcao.equalsIgnoreCase("-bin")) {
						formatoDestino = FORMATO_BIN;
					} else if (opcao.equalsIgnoreCase("-cas")) {
						formatoDestino = FORMATO_CAS;
					} else if (opcao.equalsIgnoreCase("-wav")) {
						formatoDestino = FORMATO_WAV;
					} else if (opcao.equalsIgnoreCase("-list")) {
						formatoDestino = FORMATO_BAS;
						usarStdout = true;
					}
					if (formatoDestino != FORMATO_INDEFINIDO) {
						// Foi informada uma opção de formato de destino.
						// Calcular nome do arquivo de destino a partir do nome do arquivo de origem.
						nomeDoArquivoDestino = m.group(1) +
							(formatoDestino != FORMATO_BAS && m.group(2) != null ? m.group(2) : "") +
							(formatoOrigem == FORMATO_BAS ? "." + m.group(3) : "") +
							(indiceDeArquivoEmCasseteAExtrairInformado ? "(" + indiceDeArquivoEmCasseteAExtrair + ")": "") +
							"." +
							(formatoDestino == FORMATO_BAS ? "bas" :
							 formatoDestino == FORMATO_BIN ? "bin" :
							 formatoDestino == FORMATO_CAS ? "cas" : "wav");
					} else {
						// Não foi informada uma opção de formato de destino.
						// Obter o nome do arquivo de destino da linha de comando.
						nomeDoArquivoDestino = args[numarg];
						m = p.matcher(nomeDoArquivoDestino);
						if (m.find()) {
							if ((extensaoDoArquivoDestino = m.group(3)).equalsIgnoreCase("bas")) {
								formatoDestino = FORMATO_BAS;
							} else if (extensaoDoArquivoDestino.equalsIgnoreCase("bin")) {
								formatoDestino = FORMATO_BIN;
							} else if (extensaoDoArquivoDestino.equalsIgnoreCase("cas")) {
								formatoDestino = FORMATO_CAS;
							} else /* if (extensaoDoArquivoDestino.equalsIgnoreCase("wav")) */ {
								formatoDestino = FORMATO_WAV;
							}
						} else {
							System.err.println(">>>! Extensão do arquivo de destino não reconhecida, deve ser BAS/BIN/CAS/WAV.");
							System.exit(1);
						}
					}
					if (formatoDestino == formatoOrigem) {
						System.err.println(">>>! Formato de destino igual ao formato de origem.");
						System.exit(1);
					}
					sintaxeIncorreta = false;
				}
			} else {
				System.err.println(">>>! Extensão do arquivo de origem não reconhecida, deve ser BAS/TXT/BIN/CAS/WAV.");
				System.exit(1);
			}
		}
		if (sintaxeIncorreta) {
			System.err.println(
				"Uso:\n\n" +
				"   java MC1000CasTools <opções> <arq_origem> -<formato>\n" +
				"   java MC1000CasTools <opções> <arq_origem> -list\n" +
				"   java MC1000CasTools <opções> <arq_origem> <arq_destino>\n\n" +
				"Converte arquivos entre formatos diferentes:\n\n" +
				"   *.bas : Arquivo contendo código-fonte de programa BASIC do MC-1000.\n" +
				"   *.bin : Arquivo contendo dados brutos de um bloco de memória (no caso de um\n" +
				"        programa BASIC, é o programa em formato tokenizado).\n" +
				"   *.cas : Arquivo contendo dados tal como gerados/lidos pelos comandos SAVE,\n" +
				"        LOAD e TLOAD do MC-1000: cabeçalho (nome de arquivo em cassete,\n" +
				"        endereço de início, endereço de fim) + dados brutos.\n" +
				"   *.wav : Som produzido pelo MC-1000 correspondente aos dados de cassete.\n\n" +
				"Opções:\n\n" +
				"   -b : Na conversão de BIN para CAS ou WAV, indica que o conteúdo do arquivo\n" +
				"        é um programa BASIC, para que o nome de arquivo em cassete seja\n" +
				"        adequadamente formatado (até 5 caracteres, completados com espaços).\n" +
				"   -n <nome> : Na conversão de BAS ou BIN para CAS ou WAV, especifica o nome de\n" +
				"        arquivo em cassete (até 14 caracteres). O valor predefinido é vazio.\n" +
				"   -i <número> : Se um arquivo WAV contém mais de um arquivo de cassete, indica\n" +
				"        qual deles converter. O valor predefinido é 1.\n" +
				"   -v : Modo verboso. Exibe diversas informações sobre o processo de conversão.\n\n" +
				"Outros parâmetros:\n\n" +
				"   <arq_origem> : O arquivo a ser convertido. O formato será reconhecido pela\n" +
				"        extensão.\n" +
				"   -<formato> : O formato final da conversão: -bas, -bin, -cas ou -wav.\n" +
				"        Se esta opção for usada, o nome do arquivo de destino será o mesmo do\n" +
				"        arquivo de origem com a extensão devidamente modificada.\n" +
				"   -list : Converte para formato BASIC e exibe na tela, sem gerar arquivo.\n" +
				"   <arq_destino> : Se não for especificado um formato de conversão, deve-se\n" +
				"        fornecer o nome do arquivo de destino. O formato da conversão será\n" +
				"        detectado pela extensão.\n\n" +
				"Nota: Esta ferramenta extrai apenas o primeiro arquivo de cassete contido em um\n" +
				"arquivo WAV. Se houver mais arquivos de cassete, o usuário deverá dividir o\n" +
				"arquivo WAV em arquivos menores por conta própria.\n\n" +
				"Para maiores informações visite:\n" +
				"https://sites.google.com/site/ccemc1000/sistema/cassete"
			);
			System.exit(1);
		}
		if (modoVerboso) if (!usarStdout) System.out.printf("Arquivo de destino: [%s].\n", nomeDoArquivoDestino);
		
		// Conecta os componentes da conversão.
		InputStream entrada = new BufferedInputStream(new FileInputStream(arquivoOrigem));
		if (formatoOrigem < formatoDestino) {
			// Conversão no sentido BAS->BIN->CAS->WAV.
			if (formatoOrigem == FORMATO_BAS) entrada = new Bas2Bin(entrada);
			if (formatoOrigem <= FORMATO_BIN && formatoDestino >= FORMATO_CAS) entrada = new Bin2Cas(entrada);
			if (formatoDestino == FORMATO_WAV) entrada = new Cas2Wav(entrada);
		} else {
			// Conversão no sentido WAV->CAS->BIN->WAV.
			if (formatoOrigem == FORMATO_WAV) entrada = new Wav2Cas(new BufferedInputStream(entrada));
			if (formatoOrigem >= FORMATO_CAS && formatoDestino <= FORMATO_BIN) entrada = new Cas2Bin(entrada);
			if (formatoDestino == FORMATO_BAS) entrada = new Bin2Bas(entrada);
		}
		OutputStream saida = usarStdout ? System.out : new FileOutputStream(nomeDoArquivoDestino);
		
		// Lê bytes para o arquivo de saída.
		int _byte;
		while((_byte = entrada.read()) != -1) {
			saida.write(_byte);
		}
		
		// Fecha e termina.
		entrada.close();
		saida.close();
	}

	// CONVERSÃO BASIC --> BINÁRIO.

	private static class Bas2Bin extends InputStream {
		private static final int ESTRATEGIA_TOKEN = 0;
		private static final int ESTRATEGIA_DATA = 1;
		private static final int ESTRATEGIA_DATA2 = 2;
		private static final int ESTRATEGIA_DATA3 = 3;
		private static final int ESTRATEGIA_REM = 4;
		private static final int ESTRATEGIA_STRING_OU_CARACTER = 5;
		private static final int ESTRATEGIA_STRING = 6;
		private static final int ESTRATEGIA_CARACTER = 7;
		
		private static final Pattern pLinhaComNumero = Pattern.compile("^\\s*(\\d+)\\s*(.*)");
		
		private BufferedReader entrada;
		private StringBuffer linhaBin;
		private boolean fimDeDados;
		private String linhaBas;
		private int enderecoDaProximaLinha;
		private char caracter;
		private int estrategia;
		private int estrategiaAntesDeSTRING;

		public Bas2Bin(InputStream entrada) {
			this.entrada = new BufferedReader(new InputStreamReader(entrada));
			fimDeDados = false;
			linhaBas = "";
			linhaBin = null;
			enderecoDaProximaLinha = 0x03d5; // Endereço padrão do início do programa BASIC na memória.
		}

		@Override
		public int read() throws IOException {
			// Enquanto houver bytes no buffer de saída, fornece-os.
			if (linhaBin != null && linhaBin.length() != 0) {
				int c = (int) (linhaBin.charAt(0) & 0xff);
				linhaBin.deleteCharAt(0);
				return c;
			}
			// Retorna -1 se não houver mais dados de entrada.
			if (fimDeDados) {
				return -1;
			}
			// Procura uma linha de programa BASIC válida.
			int numeroDaLinha;
			while (true) {
				// Tenta ler uma linha.
				if (fimDeDados = ((linhaBas = entrada.readLine()) == null)) {
					// Não há mais linhas:
					// 2 bytes 0 indicam o fim do programa.
					// O comando SAVE salva um byte extra ao fim.
					linhaBin.append("\0\0\0");
					return read();
				}
				if (modoVerboso) System.out.printf("Bas2Bin> %s\n", linhaBas);
				// Checa se a linha consiste de número de linha (0~65535) + conteúdo.
				Matcher m = pLinhaComNumero.matcher(linhaBas);
				if (m.find()) {
					// Extrai o número da linha.
					if (m.group(1).length() > 5 || ((numeroDaLinha = Integer.parseInt(m.group(1))) > 65535)) {
						// Descarta linha se número inválido.
						System.err.println("Bas2Bin>! Linha ignorada: Número de linha ausente ou maior que 65535.");
					} else {
						// Número válido. Converte o conteúdo para maiúsculas e conclui a busca.
						linhaBas = m.group(2).toUpperCase();
						break;
					}
				} else {
					System.err.println("Bas2Bin>! Linha ignorada: Numero de linha ausente.");
				}
			}
			// Buffer que conterá a linha tokenizada:
			// 2 bytes de endereço da próxima linha
			// + 2 bytes de número da linha
			// + conteúdo da linha
			// + 1 byte 0 ao fim da linha.
			linhaBin = new StringBuffer();
			
			// Endereço da próxima linha (a definir mais tarde).
			linhaBin.append("\0\0");
			
			// Número da linha.
			linhaBin.append((char) ( numeroDaLinha       & 0xff));
			linhaBin.append((char) ((numeroDaLinha >> 8) & 0xff));
			
			// Começa tokenizando o conteúdo da linha.
			estrategia = ESTRATEGIA_TOKEN;
			
			while (!linhaBas.isEmpty()) {
				caracter = linhaBas.charAt(0);
				linhaBas = linhaBas.substring(1);
				exec(estrategia);
			}
			
			// Byte 0 termina a linha.
			linhaBin.append('\0');
			
			// Atualiza o endereço da próxima linha no início do buffer de saída.
			enderecoDaProximaLinha = enderecoDaProximaLinha + linhaBin.length();
			linhaBin.setCharAt(0, (char) ( enderecoDaProximaLinha       & 0xff));
			linhaBin.setCharAt(1, (char) ((enderecoDaProximaLinha >> 8) & 0xff));
			
			return read();
		}

		private void exec(int estrategiaExec) throws IOException {
			switch (estrategiaExec) {
			case ESTRATEGIA_TOKEN: // Estratégia padrão: Descarta espaços e tokeniza palavras reservadas.
				switch (caracter) {
				case ' ':
					// Descarta espaços.
					break;
				default:
					// Verifica se encontrou uma palavra reservada.
					String linhaBasTmp = String.valueOf(caracter) + linhaBas; // Restaura o caracter extraído da linha.
					String palavraReservada = null;
					char token = '\0';
					for (int i = 0; i < palavrasReservadas.length; i++) {
						palavraReservada = palavrasReservadas[i];
						if (linhaBasTmp.startsWith(palavraReservada)) {
							token = (char) (i ^ 0x80);
							break;
						}
					}
					if (token != '\0') {
						// Tokeniza.
						linhaBin.append(token);
						// Pula as letras da palavra reservada.
						linhaBas = linhaBas.substring(palavraReservada.length() - 1);
						// Certas palavras reservadas implicam mudança de estratégia.
						if (palavraReservada.equals("DATA")) {
							estrategia = ESTRATEGIA_DATA;
						} else if (palavraReservada.equals("REM") || palavraReservada.equals("SAVE") || palavraReservada.equals("LOAD")) {
							estrategia = ESTRATEGIA_REM;
						}
					} else {
						// Outros caracteres.
						exec(ESTRATEGIA_STRING_OU_CARACTER);
					}
				}
				break;
			case ESTRATEGIA_DATA: // Trata o início de uma instrução DATA: Descarta primeiro espaço, se houver.
				switch (caracter) {
				case ' ':
					// Descarta primeiro espaço após a palavra "DATA".
					estrategia = ESTRATEGIA_DATA2;
					break;
				default:
					estrategia = ESTRATEGIA_DATA2;
					exec(ESTRATEGIA_DATA2);
					break;
				}
				break;
			case ESTRATEGIA_DATA2: // Trata caracteres entre dados em uma instrução DATA: espaços, vírgulas...
				exec(ESTRATEGIA_CARACTER);
				switch (caracter) {
				case ' ':
				case ',':
					// Espaço ou vírgula não mudam a estratégia.
					break;
				case ':':
					// Fim da instrução DATA.
					estrategia = ESTRATEGIA_TOKEN;
					break;
				default:
					// Encontrou algum dado.
					estrategia = ESTRATEGIA_DATA3;
					break;
				}
				break;
			case ESTRATEGIA_DATA3: // Trata dado em instrução DATA.
				exec(ESTRATEGIA_STRING_OU_CARACTER);
				switch (caracter) {
				case ',':
					// Volta a tratar caracteres entre dados.
					estrategia = ESTRATEGIA_DATA2;
					break;
				case ':':
					// Fim da instrução DATA.
					estrategia = ESTRATEGIA_TOKEN;
					break;
				}
				break;
			case ESTRATEGIA_REM: // Trata o início de uma instrução REM/SAVE/LOAD: Descarta primeiro espaço, se houver. 
				switch (caracter) {
				case ' ':
					// Descarta primeiro espaço após a palavra "REM".
					estrategia = ESTRATEGIA_CARACTER;
					break;
				default:
					estrategia = ESTRATEGIA_CARACTER;
					exec(ESTRATEGIA_CARACTER);
					break;
				}
				break;
			case ESTRATEGIA_STRING_OU_CARACTER: // Identifica início de string.
				exec(ESTRATEGIA_CARACTER);
				switch (caracter) {
				case '"':
					// Inicio de string.
					// Salva estratégia atual para voltar ao final da string.
					estrategiaAntesDeSTRING = estrategia;
					estrategia = ESTRATEGIA_STRING;
					break;
				}
				break;
			case ESTRATEGIA_STRING: // Trata o corpo de uma string até encontrar aspas.
				exec(ESTRATEGIA_CARACTER);
				switch (caracter) {
				case '"':
					// Fim da string.
					estrategia = estrategiaAntesDeSTRING;
					break;
				}
				break;
			case ESTRATEGIA_CARACTER: // Trata caracteres.
				switch (caracter) {
				case '~':
					// Notação hexadecimal ~XX.
					if (linhaBas.matches("^[0-9A-Fa-f]{2}")) {
						linhaBin.append(Integer.parseInt(linhaBas.substring(0, 2), 16));
						linhaBas = linhaBas.substring(2);
					} else {
						linhaBin.append(caracter);
					}
					break;
				case '`':
					// Notação de caracter inverso: `X.
					if (linhaBas.length() >= 1) {
						linhaBin.append((char) (linhaBas.charAt(0) ^ 0x80));
						linhaBas = linhaBas.substring(1);
					} else {
						linhaBin.append(caracter);
					}
					break;
				default:
					linhaBin.append(caracter);
				}
				break;
			}
		}
	}
	
	// CONVERSÃO BINÁRIO --> CASSETE.
	
	private static class Bin2Cas extends InputStream {
		private InputStream entrada;
		private StringBuffer dados = null;
		private StringBuffer cabecalho;
		
		public Bin2Cas(InputStream entrada) {
			this.entrada = entrada;
		}
		
		@Override
		public int read() throws IOException {
			int _byte;
			
			if (dados == null) {
				dados = new StringBuffer();
				cabecalho = new StringBuffer();
				
				// Carrega o bloco de dados.
				while ((_byte = entrada.read()) != -1) {
					dados.append((char) _byte);
				}
				
				// Ajusta nome de arquivo em cassete.
				if (eProgramaBASIC) {
					// Se o arquivo é um programa em BASIC, limita nome de arquivo em cassete a 5 caracteres, preenchidos com espaços.
					nomeDeArquivoEmCassete = (nomeDeArquivoEmCassete.toUpperCase() + "     ").substring(0, 5);
				}
				if (nomeDeArquivoEmCassete.length() < 14) {
					// Acrescenta CR ao fim quando nome de arquivo em cassete tem menos de 14 caracteres.
					nomeDeArquivoEmCassete = nomeDeArquivoEmCassete + "\r";
				} else if (nomeDeArquivoEmCassete.length() > 14) {
					// Limita o tamanho do nome de arquivo em cassete a 14 caracteres.
					nomeDeArquivoEmCassete = nomeDeArquivoEmCassete.substring(0, 14);
				}
				
				// Compõe cabeçalho:
				// 1. Nome de arquivo em cassete:
				cabecalho.append(nomeDeArquivoEmCassete);
				// 2. Endereço inicial do bloco de dados (0x0000):
				cabecalho.append("\0\0");
				// 3. Endereço final do bloco de dados (= tamanho do bloco):
				cabecalho.append((char) ( dados.length()       & 0xff)); // LSB.
				cabecalho.append((char) ((dados.length() >> 8) & 0xff)); // MSB.
				
				return read();
			} else if (cabecalho.length() != 0) {
				_byte = cabecalho.charAt(0);
				cabecalho.deleteCharAt(0);
				return _byte;
			} else if (dados.length() != 0) {
				_byte = dados.charAt(0);
				dados.deleteCharAt(0);
				return _byte;
			}
			return -1;
		}
	}
	
	// CONVERSÃO CASSETE --> WAV.
	
	private static class Cas2Wav extends InputStream {
		private InputStream entrada;
		private ArrayList<Byte> listaDeAmostras;
		private InputStream dados = null;
		
		public Cas2Wav(InputStream entrada) {
			this.entrada = entrada;
		}

		@Override
		public int read() throws IOException {
			if (dados == null) {
				// Produz amostras num ArrayList, para poder saber a quantidade.
				listaDeAmostras = new ArrayList<Byte>();
				
				// Gera sinal piloto:
				// 4096 períodos curtos.
				for (int i = 0; i < 4096; i++) {
					periodoCurto();
				}
				// 256 períodos longos.
				for (int i = 0; i < 256; i++) {
					periodoLongo();
				}

				// Converte os bytes em sinais sonoros.
				int _byte;
				while((_byte = entrada.read()) != -1) {
					gravaByte(_byte);
				}
				
				// Algumas amostras baixas no final para fazer contraste
				// com as amostras altas no final dos dados.
				amostrasBaixas();
				amostrasBaixas();
				amostrasBaixas();
				amostrasBaixas();
				
				// Agora podemos criar um array de bytes.
				byte[] amostras = new byte[listaDeAmostras.size()];
				for (int i = 0; i < listaDeAmostras.size(); i++) {
					amostras[i] = listaDeAmostras.get(i).byteValue();
				}
				
				final AudioFormat.Encoding codificacao = AudioFormat.Encoding.PCM_SIGNED;
				final float taxaDeAmostrasPorSegundo = 11025;
				final int tamanhoDaAmostraEmBits = 8;
				final int qtdCanais = 1;
				final int tamanhoDoQuadroEmBytes = qtdCanais * tamanhoDaAmostraEmBits / 8;
				final float taxaDeQuadrosPorSegundo = taxaDeAmostrasPorSegundo;
				final boolean bigEndian = false;
				
				AudioFormat formato = new AudioFormat(
					codificacao,
					taxaDeAmostrasPorSegundo,
					tamanhoDaAmostraEmBits,
					qtdCanais,
					tamanhoDoQuadroEmBytes,
					taxaDeQuadrosPorSegundo,
					bigEndian
				);
				
				ByteArrayInputStream fluxoDeEntradaDeArrayDeBytes = new ByteArrayInputStream(amostras);
				
				AudioInputStream fluxoDeEntradaDeAudio = new AudioInputStream(
					fluxoDeEntradaDeArrayDeBytes,
					formato,
					amostras.length
				);
				
				ByteArrayOutputStream fluxoDeSaidaDeAudio = new ByteArrayOutputStream();
				
				AudioSystem.write(fluxoDeEntradaDeAudio, AudioFileFormat.Type.WAVE, fluxoDeSaidaDeAudio);
				
				dados = new ByteArrayInputStream(fluxoDeSaidaDeAudio.toByteArray());
				
				return read();
			} else {
				return dados.read();
			}
			//return -1;
		}
		
		private void gravaByte(int _byte) {
			// Marca de início de byte.
			periodoCurto();
			// Os oito bits do byte, do menos para o mais significativo.
			boolean par = true;
			for (int i = 0; i < 8; i++) {
				if ((_byte & (1 << i)) != 0) {
					// Bit 1.
					periodoCurto();
					par = !par;
				} else {
					// Bit 0.
					periodoLongo();
				}
			}
			// Bit de paridade.
			if (par) {
				periodoCurto();
			} else {
				periodoLongo();
			}
		}
		
		private void periodoCurto() {
			amostrasBaixas();
			amostrasAltas();
		}
		
		private void periodoLongo() {
			amostrasBaixas();
			amostrasBaixas();
			amostrasAltas();
			amostrasAltas();
		}
		
		private void amostrasBaixas() {
			// 4 amostras a 11025 amostras por segundo.
			listaDeAmostras.add(new Byte((byte) -128));
			listaDeAmostras.add(new Byte((byte) -128));
			listaDeAmostras.add(new Byte((byte) -128));
			listaDeAmostras.add(new Byte((byte) -128));
		}
		
		private void amostrasAltas() {
			// 4 amostras a 11025 amostras por segundo.
			listaDeAmostras.add(new Byte((byte) +127));
			listaDeAmostras.add(new Byte((byte) +127));
			listaDeAmostras.add(new Byte((byte) +127));
			listaDeAmostras.add(new Byte((byte) +127));
		}
	}

	// CONVERSÃO WAV --> CASSETE.
	
	private static class Wav2Cas extends InputStream {
		private InputStream entrada;
		private float duracaoDaAmostra;
		private byte[] amostras = null;
		private int indiceAmostras = 0;
		private byte amostraMax = 0;
		private byte amostraMin = 0;
		private float fatorDeAmplitudeDasAmostras = 0;

		public Wav2Cas(InputStream entrada) {
			this.entrada = entrada;
		}

		@Override
		public int read() throws IOException {
			if (amostras == null) {
				// Código adaptado de
				// http://stackoverflow.com/questions/938304/how-to-get-audio-data-from-a-mp3
				
				AudioInputStream fluxoDeEntrada = null;
				try {
					fluxoDeEntrada = AudioSystem.getAudioInputStream(entrada);
				} catch (UnsupportedAudioFileException e) {
					e.printStackTrace();
					System.exit(1);
				}
				AudioFormat formatoBase = fluxoDeEntrada.getFormat();
				if (modoVerboso) System.out.printf("Wav2Cas> %s\n", formatoBase.toString());
	
				AudioInputStream fluxoDeEntradaDecodificado;
				AudioFormat formatoDecodificado;
	
				if ((formatoBase.getEncoding() == AudioFormat.Encoding.PCM_SIGNED)
						|| (formatoBase.getEncoding() == AudioFormat.Encoding.PCM_UNSIGNED)) {
					// Já é PCM, não converter.
					fluxoDeEntradaDecodificado = fluxoDeEntrada;
					formatoDecodificado = formatoBase;
				} else {
					// Outro formato. Tentar converter.
					formatoDecodificado = new AudioFormat(
						AudioFormat.Encoding.PCM_SIGNED,
						formatoBase.getSampleRate(),
						16,
						formatoBase.getChannels(),
						formatoBase.getChannels() * 2,
						formatoBase.getSampleRate(),
						false);
					if (modoVerboso) System.out.printf("Wav2Cas> %s\n", formatoDecodificado.toString());
					fluxoDeEntradaDecodificado = AudioSystem.getAudioInputStream(formatoDecodificado, fluxoDeEntrada);
				}
				
				final int tamanhoDaAmostraEmBytes = formatoDecodificado.getSampleSizeInBits() / 8;
				final int tamanhoDoQuadroEmBytes = formatoDecodificado.getChannels() * tamanhoDaAmostraEmBytes;
				
				amostras = new byte[(int) fluxoDeEntradaDecodificado.getFrameLength()];
				indiceAmostras = 0;
				amostraMax = 0;
				amostraMin = 0;
				fatorDeAmplitudeDasAmostras = 0;
				
				// Carrega todas as amostras .
				int numBytesRead = 0;
				byte[] audioBytes = new byte[tamanhoDoQuadroEmBytes];
				while ((numBytesRead = fluxoDeEntradaDecodificado.read(audioBytes)) != -1) {
					if (numBytesRead == 0) break;
					
					byte amostra = 0;
					
					if (tamanhoDaAmostraEmBytes == 1) {
						amostra = audioBytes[0];
					} else {
						amostra = audioBytes[formatoDecodificado.isBigEndian() ? 0 : (tamanhoDaAmostraEmBytes - 1)];
					}
					if (formatoDecodificado.getEncoding() == AudioFormat.Encoding.PCM_UNSIGNED) {
						amostra = (byte) ((amostra & 0xff) - 0x80);
					}
					
					amostras[indiceAmostras++] = amostra;
					// Identifica os valores mínimo e máximo para normalizar posteriormente.
					if (amostra > amostraMax) amostraMax = amostra;
					if (amostra < amostraMin) amostraMin = amostra;
				}
				
				if (modoVerboso) {
					System.out.println("Wav2Cas> Análise concluída.");
					System.out.printf("Wav2Cas> Quantidade de frames: %d.\n", fluxoDeEntradaDecodificado.getFrameLength());
					System.out.printf("Wav2Cas> Amostras obtidas: %d.\n", indiceAmostras);
					System.out.printf("Wav2Cas> Mínimo: %d.\n", amostraMin);
					System.out.printf("Wav2Cas> Máximo: %d.\n", amostraMax);
				}
				
				if (indiceAmostras == 0) return -1;
				
				fatorDeAmplitudeDasAmostras = 255 / (amostraMax - amostraMin); // Para adiantar cálculo de normalização.
				
				duracaoDaAmostra = 1 / formatoDecodificado.getFrameRate();
				
				indiceAmostras = 0;
				trataInicioDasAmostras();
				
				return read();
			} else if (fimDeArquivo) {
				return -1;
			} else {
				while (indiceAmostras < amostras.length) {
					// Normaliza a amostra para o intervalo -128 ~ +127:
					amostra = /* amostras[indiceAmostras] = */
						(byte) ((amostras[indiceAmostras] - amostraMin) * fatorDeAmplitudeDasAmostras - 128);
					
					if (modoVerboso) System.out.printf(
							"Amostra %d:%s# %d\n",
							indiceAmostras,
							"                                                  ".substring(0, (amostra + 128) * 50 / 255),
							amostra
					);
					
					indiceAmostras++;
					trataAmostra();
					if (fimDeByte) {
						fimDeByte = false;
						if (extrairEsteArquivo) {
							return _byte;
						}
					}
				}
				trataFimDasAmostras();
			}
			return -1;
		}
		
		static final int PROCURANDO_64_PULSOS_CURTOS = 0;
		static final int LENDO_64_PULSOS_CURTOS = 1;
		static final int PROCURANDO_64_PULSOS_LONGOS = 2;
		static final int LENDO_64_PULSOS_LONGOS = 3;
		static final int PROCURANDO_INICIO_DO_BYTE = 4;
		static final int LENDO_INICIO_DO_BYTE = 5;
		static final int LENDO_BYTE = 6;
		static final int LENDO_PARIDADE = 7;
		
		static final int PULSO_CURTO_DEMAIS = 2;
		static final int PULSO_CURTO_BIT_1 = 1; // = Bit 1.
		static final int PULSO_LONGO_BIT_0 = 0; // = Bit 0.
		static final int PULSO_LONGO_DEMAIS = -1;
		
		// Aqui o termo "pulso" refere-se a uma sequência de amostras altas.
		private double duracaoDoPulso;
		// No MC-1000, um pulso curto representa o bit 1, um pulso longo
		// representa o bit 0.
		// Um pulso curto ocupa cerca de 16 amostras em um arquivo
		// com 44100 amostras por segundo, aprox. 3.628118E-4 segundos.
		static final double duracaoDoPulsoCurto = 16.0d / 44100;
		// Um pulso longo dura o dobro do tempo.
		// Limites das faixas usadas para reconhecer pulsos curtos,
		// pulsos longos, e ruído (pulsos curtos demais ou longos demais).
		static final double duracaoMinDoPulsoCurto = duracaoDoPulsoCurto * Math.pow(2, -1.0);
		static final double duracaoMaxDoPulsoCurto = duracaoDoPulsoCurto * Math.pow(2, +0.5);
		static final double duracaoMaxDoPulsoLongo = duracaoDoPulsoCurto * Math.pow(2, +2.0);
		
		private int estado;
		private byte amostra;
		private int sinalDaAmostra;
		private byte valorDoPulso;
		private int contadorDePulsos;
		private int _byte;
		private boolean par;
		private boolean fimDeByte = false;
		private int indiceDeArquivoEmCassete = 0;
		private boolean extrairEsteArquivo = false;
		private boolean fimDeArquivo = false;
		
		private void trataInicioDasAmostras() {
			estado = PROCURANDO_64_PULSOS_CURTOS;
			sinalDaAmostra = -1;
		}
		
		private void trataAmostra() {
			if (sinalDaAmostra == -1) {
				if (amostra >= 0) {
					iniciaPulso();
				}
			} else  {
				if (amostra >= 0) {
					duracaoDoPulso += duracaoDaAmostra;
				} else {
					emitePulso();
				}
			}
		}
		
		private void trataFimDasAmostras() {
			if (sinalDaAmostra == +1) {
				emitePulso();
			}
			trataFimDosPulsos();
		}
		
		private void iniciaPulso() {
			sinalDaAmostra = +1;
			duracaoDoPulso = duracaoDaAmostra;
		}
		
		private void emitePulso() {
			if (duracaoDoPulso < duracaoMinDoPulsoCurto) {
				valorDoPulso = PULSO_CURTO_DEMAIS;
			} else if (duracaoDoPulso < duracaoMaxDoPulsoCurto) {
				valorDoPulso = PULSO_CURTO_BIT_1;
			} else if (duracaoDoPulso < duracaoMaxDoPulsoLongo) {
				valorDoPulso = PULSO_LONGO_BIT_0;
			} else {
				valorDoPulso = PULSO_LONGO_DEMAIS;
			}
			trataPulso();
			sinalDaAmostra = -1;
		}
		
		private void trataPulso() {
			switch (estado) {
			case PROCURANDO_64_PULSOS_CURTOS:
				if (valorDoPulso == PULSO_CURTO_BIT_1) {
					contadorDePulsos = 1;
					estado = LENDO_64_PULSOS_CURTOS;
				}
				break;
			case LENDO_64_PULSOS_CURTOS:
				if (valorDoPulso == PULSO_CURTO_BIT_1) {
					if (++contadorDePulsos == 64) {
						if (modoVerboso) System.out.println("Wav2Cas> Tom piloto, parte 1/2: Lidos 64 pulsos curtos.");
						estado = PROCURANDO_64_PULSOS_LONGOS;
					}
				} else {
					// Pulso estranho na sequencia.
					// Reinicia a contagem.
					estado = PROCURANDO_64_PULSOS_CURTOS;
				}
				break;
			case PROCURANDO_64_PULSOS_LONGOS:
				if (valorDoPulso == PULSO_LONGO_BIT_0) {
					contadorDePulsos = 1;
					estado = LENDO_64_PULSOS_LONGOS;
				}
				break;
			case LENDO_64_PULSOS_LONGOS:
				if (valorDoPulso == PULSO_LONGO_BIT_0) {
					if (++contadorDePulsos == 64) {
						if (modoVerboso) System.out.println("Wav2Cas> Tom piloto, parte 2/2: Lidos 64 pulsos longos.");
						estado = PROCURANDO_INICIO_DO_BYTE;
					}
				} else {
					// Pulso estranho na sequencia.
					// Reinicia a contagem.
					estado = PROCURANDO_64_PULSOS_LONGOS;
				}
				break;
			case PROCURANDO_INICIO_DO_BYTE:
				if (valorDoPulso == PULSO_CURTO_BIT_1) {
					trataInicioDosBytes();
					iniciaByte();
				}
				break;
			case LENDO_INICIO_DO_BYTE:
				if (valorDoPulso == PULSO_CURTO_DEMAIS) {
					System.err.println("Wav2Cas>! Pulso de início de byte curto demais!");
				} else if (valorDoPulso == PULSO_LONGO_BIT_0 || valorDoPulso == PULSO_LONGO_DEMAIS) {
					System.err.println("Wav2Cas>! Pulso de início de byte longo demais!"); 
				}
				iniciaByte();
				break;
			case LENDO_BYTE:
				switch (valorDoPulso) {
				case PULSO_LONGO_DEMAIS:
					System.err.println("Wav2Cas>! Pulso de bit longo demais!");
				case PULSO_LONGO_BIT_0:
					break;
				case PULSO_CURTO_DEMAIS:
					System.err.println("Wav2Cas>! Pulso de bit curto demais!");
				case PULSO_CURTO_BIT_1:
					_byte |= 1 << contadorDePulsos;
					par = !par;
					break;
				}
				if (++contadorDePulsos == 8) {
					estado = LENDO_PARIDADE;
				}
				break;
			case LENDO_PARIDADE:
				switch (valorDoPulso) {
				case PULSO_LONGO_DEMAIS:
					System.err.println("Wav2Cas>! Pulso de bit de paridade longo demais!");
				case PULSO_LONGO_BIT_0:
					if (par) {
						System.err.println("Wav2Cas>! Erro de paridade.");
					}
					break;
				case PULSO_CURTO_DEMAIS:
					System.err.println("Wav2Cas>! Pulso de bit de paridade curto demais!");
				case PULSO_CURTO_BIT_1:
					if (!par) {
						System.err.println("Wav2Cas>! Erro de paridade.");
					}
				}
				emiteByte();
			}
		}
		
		private void trataFimDosPulsos() {
			switch (estado) {
			case PROCURANDO_64_PULSOS_CURTOS:
			case LENDO_64_PULSOS_CURTOS:
			case PROCURANDO_64_PULSOS_LONGOS:
			case LENDO_64_PULSOS_LONGOS:
			case PROCURANDO_INICIO_DO_BYTE:
			case LENDO_INICIO_DO_BYTE:
				break;
			case LENDO_BYTE:
			case LENDO_PARIDADE:
				emiteByte();
				break;
			}
			trataFimDosBytes();
		}
		
		static final int PROCURANDO_ARQUIVO = 0;
		static final int LENDO_NOME_DO_ARQUIVO = 1;
		static final int LENDO_ENDERECO_DE_INICIO = 2;
		static final int LENDO_ENDERECO_DE_FIM = 3;
		static final int LENDO_BLOCO_DE_DADOS = 4;
		
		private int estado2;
		private StringBuffer nomeDeArquivoEmCassete;
		private int enderecoDeInicio;
		private int enderecoDeFim;
		private int tamanhoDoBlocoDeDados;
		// private byte[] blocoDeDados;
		private long contadorDeBytes;
		
		private void trataInicioDosBytes() {}
		
		private void iniciaByte() {
			estado = LENDO_BYTE;
			contadorDePulsos = 0;
			_byte = 0;
			par = true;
		}
		
		private void emiteByte() {
			fimDeByte = true;
			if (modoVerboso) System.out.printf("Wav2Cas> Byte: 0x%s = %d = %s\n", Integer.toHexString(_byte), _byte, (char) (_byte < ' ' ? ' ' : _byte));
			trataByte();
			if (estado2 == PROCURANDO_ARQUIVO) {
				estado = PROCURANDO_64_PULSOS_CURTOS;
			} else {
				estado = LENDO_INICIO_DO_BYTE;
			}
		}
		
		private void trataFimDosBytes() {
			if (estado2 == LENDO_BLOCO_DE_DADOS) {
				emiteArquivo();
			}
		}
		
		private void iniciaArquivo() {
			extrairEsteArquivo = (++indiceDeArquivoEmCassete == indiceDeArquivoEmCasseteAExtrair);
			nomeDeArquivoEmCassete = new StringBuffer();
			enderecoDeInicio = 0x0000;
			enderecoDeFim = 0x0000;
			tamanhoDoBlocoDeDados = 0;
			// blocoDeDados = null;
			estado2 = LENDO_NOME_DO_ARQUIVO;
			contadorDeBytes = 0;
			trataByte();
		}
		
		private void trataByte() {
			switch (estado2) {
			case PROCURANDO_ARQUIVO:
				iniciaArquivo();
				break;
			case LENDO_NOME_DO_ARQUIVO:
				if (_byte != '\r') nomeDeArquivoEmCassete.append((char) _byte);
				if (_byte == '\r' || nomeDeArquivoEmCassete.length() == 14) {
					if (modoVerboso) System.out.printf("Wav2Cas> Nome do arquivo em cassete: [%s]\n", nomeDeArquivoEmCassete);
					MC1000CasTools.nomeDeArquivoEmCassete = nomeDeArquivoEmCassete.toString();
					estado2 = LENDO_ENDERECO_DE_INICIO;
					contadorDeBytes = 0;
				}
				break;
			case LENDO_ENDERECO_DE_INICIO:
				if (++contadorDeBytes == 1) {
					enderecoDeInicio = _byte; // LSB.
				} else /* if (contadorDeBytes == 2) */ {
					enderecoDeInicio |= (_byte << 8); // MSB.
					if (modoVerboso) System.out.printf("Wav2Cas> Endereço de início: 0x%s = %d.\n", Integer.toHexString(enderecoDeInicio), enderecoDeInicio);
					estado2 = LENDO_ENDERECO_DE_FIM;
					contadorDeBytes = 0;
				}
				break;
			case LENDO_ENDERECO_DE_FIM:
				if (++contadorDeBytes == 1) {
					enderecoDeFim = _byte; // LSB.
				} else /* if (contadorDeBytes == 2) */ {
					enderecoDeFim |= (_byte << 8);
					if (modoVerboso) System.out.printf("Wav2Cas> Endereço de fim: 0x%s = %d.\n", Integer.toHexString(enderecoDeFim), enderecoDeFim);
					tamanhoDoBlocoDeDados = enderecoDeFim - enderecoDeInicio + 1;
					// blocoDeDados = new byte[tamanhoDoBlocoDeDados];
					estado2 = LENDO_BLOCO_DE_DADOS;
					contadorDeBytes = 0;
				}
				break;
			case LENDO_BLOCO_DE_DADOS:
				// blocoDeDados[(int) contadorDeBytes] = (byte) (_byte & 0xff);
				if (++contadorDeBytes == tamanhoDoBlocoDeDados) {
					emiteArquivo();
				}
				break;
			}
		}

		private void emiteArquivo() {
			fimDeArquivo = true;
			if (modoVerboso) System.out.println("Wav2Cas> Fim de arquivo em cassete.");
			estado2 = PROCURANDO_ARQUIVO;
			contadorDeBytes = 0;
		}
	}

	// CONVERSÃO CASSETE --> BINÁRIO.
	
	private static class Cas2Bin extends InputStream {
		private InputStream entrada;
		
		static final int LENDO_NOME_DO_ARQUIVO = 0;
		static final int LENDO_ENDERECO_DE_INICIO = 1;
		static final int LENDO_ENDERECO_DE_FIM = 2;
		static final int LENDO_BLOCO_DE_DADOS = 3;
		
		private int estado = LENDO_NOME_DO_ARQUIVO;

		public Cas2Bin(InputStream entrada) {
			this.entrada = entrada;
		}

		@Override
		public int read() throws IOException {
			StringBuffer nomeDeArquivoEmCassete = new StringBuffer();
			int enderecoDeInicio = 0;
			int enderecoDeFim = 0;
			int contadorDeBytes = 0;
			int _byte;
			while ((_byte = entrada.read()) != -1 && estado != LENDO_BLOCO_DE_DADOS) {
				switch (estado) {
				case LENDO_NOME_DO_ARQUIVO:
					if (_byte != '\r') nomeDeArquivoEmCassete.append((char) _byte);
					if (_byte == '\r' || nomeDeArquivoEmCassete.length() == 14) {
						if (modoVerboso) System.out.printf("Cas2Bin> Nome do arquivo em cassete: [%s]\n", nomeDeArquivoEmCassete);
						MC1000CasTools.nomeDeArquivoEmCassete = nomeDeArquivoEmCassete.toString();
						estado = LENDO_ENDERECO_DE_INICIO;
						contadorDeBytes = 0;
					}
					break;
				case LENDO_ENDERECO_DE_INICIO:
					if (++contadorDeBytes == 1) {
						enderecoDeInicio = _byte; // LSB.
					} else /* if (contadorDeBytes == 2) */ {
						enderecoDeInicio |= (_byte << 8); // MSB.
						if (modoVerboso) System.out.printf("Cas2Bin> Endereço de início: 0x%s = %d.\n", Integer.toHexString(enderecoDeInicio), enderecoDeInicio);
						estado = LENDO_ENDERECO_DE_FIM;
						contadorDeBytes = 0;
					}
					break;
				case LENDO_ENDERECO_DE_FIM:
					if (++contadorDeBytes == 1) {
						enderecoDeFim = _byte; // LSB.
					} else /* if (contadorDeBytes == 2) */ {
						enderecoDeFim |= (_byte << 8);
						if (modoVerboso) System.out.printf("Cas2Bin> Endereço de fim: 0x%s = %d.\n", Integer.toHexString(enderecoDeFim), enderecoDeFim);
						estado = LENDO_BLOCO_DE_DADOS;
						contadorDeBytes = 0;
					}
					break;
				}
			}
			return _byte;
		}
	}

	// CONVERSÃO BINÁRIO --> BASIC.
	
	private static class Bin2Bas extends InputStream {
		private InputStream entrada;
		private StringBuffer linhaBas;
		private boolean fimDeDados = false;
		
		private static final int ESTRATEGIA_TOKEN = 0;
		private static final int ESTRATEGIA_DATA = 1;
		private static final int ESTRATEGIA_DATA2 = 2;
		private static final int ESTRATEGIA_STRING_OU_CARACTER = 3;
		private static final int ESTRATEGIA_STRING = 4;
		private static final int ESTRATEGIA_CARACTER = 5;
		
		private char caracter;
		private int estrategia;
		private int estrategiaAntesDeSTRING;

		public Bin2Bas(InputStream entrada) {
			this.entrada = entrada;
		}

		@Override
		public int read() throws IOException {
			// Enquanto houver bytes no buffer de saída, fornece-os.
			if (linhaBas != null && linhaBas.length() != 0) {
				int c = (int) (linhaBas.charAt(0) & 0xff);
				linhaBas.deleteCharAt(0);
				return c;
			}
			// Retorna -1 se não houver mais dados de entrada.
			if (fimDeDados) {
				return -1;
			}
			// Procura uma linha de programa BASIC válida.
			linhaBas = new StringBuffer();
			int _byte;
			int enderecoDaProximaLinha;
			int numeroDaLinha;
			
			if (((enderecoDaProximaLinha = entrada.read()) != -1) && ((_byte = entrada.read()) != -1)) {
				enderecoDaProximaLinha |= (_byte << 8);
				if (enderecoDaProximaLinha == 0) {
					// Fim do programa.
					fimDeDados = true;
					return read();
				}
			} else {
				System.err.println("Bin2Bas>! Fim de arquivo inesperado.");
				fimDeDados = true;
				return read();
			}
			if (((numeroDaLinha = entrada.read()) != -1) && ((_byte = entrada.read()) != -1)) {
				numeroDaLinha |= (_byte << 8);
				linhaBas.append(numeroDaLinha).append(' ');
			} else {
				System.err.println("Bin2Bas>! Fim de arquivo inesperado.");
				fimDeDados = true;
				return read();
			}
			
			estrategia = ESTRATEGIA_TOKEN;
			
			while (true) {
				int c;
				if ((c = entrada.read()) != -1) {
					caracter = (char) c;
					if (caracter == '\0') {
						// Fim de linha.
						break;
					}
					exec(estrategia);
				} else {
					System.err.println("Bin2Bas>! Fim de arquivo inesperado.");
					fimDeDados = true;
					break;
				}
			}
			linhaBas.append('\n');
			if (modoVerboso) System.out.printf("Bin2Bas> %s", linhaBas.toString());
			return read();
		}
		
		private void exec(int estrategiaExec) throws IOException {
			switch (estrategiaExec) {
			case ESTRATEGIA_TOKEN: // Estratégia padrão.
				if ((caracter & 0x80) != 0 && (caracter & 0x7f) < palavrasReservadas.length) {
					// Se encontrou token, exibe palavra reservada correspondente com espaços antes e depois.
					String palavraReservada = palavrasReservadas[caracter & 0x7f];
					linhaBas.append(' ').append(palavraReservada).append(' ');
					// Certas palavras reservadas implicam mudança de estratégia.
					if (palavraReservada.equals("DATA")) {
						estrategia = ESTRATEGIA_DATA;
					} else if (palavraReservada.equals("REM") || palavraReservada.equals("SAVE") || palavraReservada.equals("LOAD")) {
						estrategia = ESTRATEGIA_CARACTER;
					}
				} else {
					switch (caracter) {
					case ' ':
						// Espaço: Introduz notação hexadecimal para que não sejam perdidos no Bas2Bin.
						linhaBas.append("~20");
						break;
					default:
						exec(ESTRATEGIA_STRING_OU_CARACTER);
						break;
					}
				}
				break;
			case ESTRATEGIA_DATA: // Trata caracteres entre dados em uma instrução DATA: espaços, vírgulas...
				exec(ESTRATEGIA_STRING_OU_CARACTER);
				switch (caracter) {
				case ' ':
				case ',':
					// Espaço ou vírgula não mudam a estratégia.
					break;
				case ':':
					// Fim da instrução DATA.
					estrategia = ESTRATEGIA_TOKEN;
					break;
				default:
					// Encontrou algum dado.
					estrategia = ESTRATEGIA_DATA2;
					break;
				}
				break;
			case ESTRATEGIA_DATA2: // Trata dado em instrução DATA.
				exec(ESTRATEGIA_STRING_OU_CARACTER);
				switch (caracter) {
				case ',':
					// Volta a tratar caracteres entre dados.
					estrategia = ESTRATEGIA_DATA;
					break;
				case ':':
					// Fim de instrução DATA.
					estrategia = ESTRATEGIA_TOKEN;
					break;
				}
				break;
			case ESTRATEGIA_STRING_OU_CARACTER: // Identifica inicio de string.
				exec(ESTRATEGIA_CARACTER);
				switch (caracter) {
				case '"':
					// Início de string.
					// Salva estratégia atual para voltar ao final da string.
					estrategiaAntesDeSTRING = estrategia;
					estrategia = ESTRATEGIA_STRING;
					break;
				}
				break;
			case ESTRATEGIA_STRING: // Trata o corpo de uma string até encontrar aspas.
				exec(ESTRATEGIA_CARACTER);
				switch (caracter) {
				case '"':
					// Fim da string.
					estrategia = estrategiaAntesDeSTRING;
					break;
				}
				break;
			case ESTRATEGIA_CARACTER: // Trata caracteres.
				if (caracter >= 32 && caracter < 96) {
					// Caracter ASCII do MC6847.
					linhaBas.append(caracter);
				} else if ((caracter ^ 0x80) >= 32 && (caracter ^ 0x80) < 96) {
					// Caracter ASCII do MC6847 inverso.
					// Listar como acento grave + caracter normal.
					linhaBas.append('`').append((char) (caracter ^ 0x80));
				} else {
					// Outros caracteres.
					// Listar como til + código hexadecimal.
					linhaBas.append('~');
					if (caracter < 0x10) linhaBas.append('0');
					linhaBas.append(Integer.toHexString(caracter).toUpperCase());
				}
				break;
			}
		}
	}
}
