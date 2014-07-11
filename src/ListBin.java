import java.io.*;

public class ListBin {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		if (args.length == 0) {
			System.err.println(
					"\nUso:\n\n" +
					"   java ListBin nomedoarquivo.bin\n\n" +
					"ListBin le um arquivo .BIN contendo  um programa em BASIC do\n" +
					"MC-1000 e reproduz o efeito do comando LIST.\n\n" +
					"Para maiores informacoes, consulte http://mc-1000.wikispaces.com/Cassete"
			);
			System.exit(0);
		}
		
		String nomeDoArquivoBin = args[0];
		print(">>> Arquivo: [");
		print(nomeDoArquivoBin);
		println("].");
		
		File arquivoBin = new File(nomeDoArquivoBin);
		if (!arquivoBin.exists()) {
			System.err.println(">>>! Arquivo não existe.");
			System.exit(0);
		}
		
		if (arquivoBin.length() == 0) {
			System.err.println(">>>! Arquivo vazio.");
			System.exit(0);
		}
		FileInputStream fis = new FileInputStream(arquivoBin);
		
		/*
		 * Lê o nome do arquivo do MC-1000.
		 * 14 caracteres ou menos.
		 * Se menos, termina com CR.
		 */
		char[] idDoArquivoTemp = new char[14];
		int i;
		for (i = 0; i < 14; i++) {
			int _byte;
			if ((_byte = fis.read()) != -1) {
				if ((char) _byte == '\r') {
					break;
				} else {
					idDoArquivoTemp[i] = (char) _byte;
				}
			} else {
				break;
			}
		}
		String idDoArquivo = new String(idDoArquivoTemp, 0, i);
		print(">>> ID do arquivo: [");
		print(idDoArquivo);
		println("].");
		
		// Pula endereço de início e de fim do bloco. (4 bytes)
		fis.read(); fis.read(); fis.read(); fis.read();
		
		// Lê o bloco de dados (programa BASIC).
		final String[] palavrasReservadas = new String[] {
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
		
		while (true) {
			int _byte;
			int enderecoDaProximaLinha;
			int numeroDaLinha;
			
			if (((enderecoDaProximaLinha = fis.read()) != -1) && ((_byte = fis.read()) != -1)) {
				enderecoDaProximaLinha |= (_byte << 8);
				if (enderecoDaProximaLinha == 0) {
					// Fim do programa.
					break;
				}
			} else {
				System.err.println(">>>! Fim de arquivo inesperado.");
				System.exit(0);
			}
			if (((numeroDaLinha = fis.read()) != -1) && ((_byte = fis.read()) != -1)) {
				numeroDaLinha |= (_byte << 8);
				print (numeroDaLinha + " ");
			} else {
				System.err.println(">>>! Fim de arquivo inesperado.");
				System.exit(0);
			}
			while (true) {
				if ((_byte = fis.read()) != -1) {
					if (_byte == 0) {
						// Fim de linha.
						println("");
						break;
					}
					if ((_byte < 128) || (_byte - 128 >= palavrasReservadas.length)) {
						// Caracter comum.
						print(String.valueOf((char) _byte));
					} else {
						// Token de palavra reservada.
						print(" ");
						print(palavrasReservadas[_byte - 128]);
						print(" ");
					}
				} else {
					System.err.println(">>>! Fim de arquivo inesperado.");
					System.exit(0);
				}
			}
		}
		// Fecha arquivos e termina.
		fis.close();
	}

	static void print(String s) {
		System.out.print(s);
	}
	
	static void println(String s) {
		System.out.println(s);
	}
	
}

/*
' LISTBIN.
'
' LISTBIN.EXE le um arquivo .BIN contendo um programa em BASIC do MC 1000
' e reproduz o efeito do comando LIST.

DECLARE SUB obtemnomearq (nomearqbin AS STRING)
DECLARE FUNCTION lebyte% ()

DIM palavrareservada(128 TO 223) AS STRING
DIM n AS INTEGER
DIM nomearqbin AS STRING
DIM posleitura AS LONG
DIM byte AS INTEGER
DIM nomearq AS STRING
DIM enderecoinicio AS LONG
DIM enderecoaposfim AS LONG
DIM enderecoproxlinha AS LONG
DIM numlinha AS LONG

FOR n = 128 TO 223
   READ palavrareservada(n)
NEXT

DATA "END", "FOR", "NEXT", "DATA", "EXIT", "INPUT", "DIM", "READ"
DATA "LET", "GOTO", "RUN", "IF", "RESTORE", "GOSUB", "RETURN", "REM"
DATA "STOP", "OUT", "ON", "HOME", "WAIT", "DEF", "POKE", "PRINT"
DATA "PR#", "SOUND", "GR", "HGR", "COLOR", "TEXT", "PLOT", "TRO"
DATA "UNPLOT", "SET", "CALL", "DRAW", "UNDRAW", "TEMPO", "WIDTH", "CONT"
DATA "LIST", "CLEAR", "LOAD", "SAVE", "NEW", "TLOAD", "COLUMN", "AUTO"
DATA "FAST", "SLOW", "EDIT", "INVERSE", "NORMAL", "DEBUG", "TAB(", "TO"
DATA "FN", "SPC(", "THEN", "NOT", "STEP", "VTAB(", "+", "-"
DATA "*", "/", "^", "AND", "OR", ">", "=", "<"
DATA "SGN", "INT", "ABS", "USR", "FRE", "INP", "POS", "SQR"
DATA "RND", "LOG", "EXP", "COS", "SIN", "TAN", "ATN", "PEEK"
DATA "LEN", "STR$", "VAL", "ASC", "CHR$", "LEFT$", "RIGHT$", "MID$"

obtemnomearq nomearqbin

OPEN nomearqbin FOR BINARY AS #1

' Le o nome do arquivo do MC 1000.
' 14 caracteres ou menos.
' Se menos, termina com CR.
nomearq = ""
posleitura = 1
DO WHILE posleitura <= 14
   byte = lebyte
   IF byte = 13 THEN EXIT DO
   nomearq = nomearq + CHR$(byte)
LOOP
PRINT ">>> Lido nome do arquivo: '"; nomearq; "'"

' Le o endereco de inicio do bloco.
enderecoinicio = lebyte + 256 * lebyte
PRINT ">>> Lido endereco de inicio do bloco: "; RIGHT$("000" + HEX$(enderecoinicio), 4)

' Le o endereco de fim do bloco.
enderecoaposfim = lebyte + 256 * lebyte
PRINT ">>> Lido endereco seguinte ao fim do bloco: "; RIGHT$("000" + HEX$(enderecoaposfim), 4)

' Le o bloco de dados propriamente dito (programa BASIC).
DO
   enderecoproxlinha = lebyte + 256 * lebyte
   IF enderecoproxlinha = 0 THEN
      ' Fim de programa.
      EXIT DO
   END IF
   numerolinha = lebyte + 256 * lebyte
   PRINT LTRIM$(STR$(numerolinha)); " ";
   DO
      byte = lebyte
      IF byte = 0 THEN
         ' Fim de linha.
         PRINT
         EXIT DO
      END IF
      IF byte < 128 THEN
         ' Caracter comum.
         PRINT CHR$(byte);
      ELSE
         ' 'Token' de palavra reservada.
         PRINT " "; palavrareservada(byte); " ";
      END IF
   LOOP
LOOP

' Fecha arquivos e termina.
CLOSE
END

FUNCTION lebyte%
   SHARED posleitura AS LONG
   DIM byte AS STRING * 1

   IF posleitura > LOF(1) THEN
      PRINT ">>> Fim de arquivo inesperado."
      END
   END IF

   GET #1, posleitura, byte
   posleitura = posleitura + 1
   lebyte = ASC(byte)
END FUNCTION

SUB obtemnomearq (nomearqbin AS STRING)
   DIM argumentos AS STRING
   DIM posicaoespaco AS INTEGER

   ' Analisa a linha de comando para obter o nome do arquivo .WAV.
   argumentos = LTRIM$(COMMAND$)
   posicaoespaco = INSTR(argumentos, " ")
   IF posicaoespaco = 0 THEN
      nomearqbin = argumentos
   ELSE
      nomearqbin = LEFT$(argumentos, posicaoespaco - 1)
   END IF

   IF RIGHT$(nomearqbin, 4) <> ".BIN" OR LEN(nomearqbin) <= 4 THEN
      ' Acusa uso incorreto.
      COLOR 7
      PRINT
      PRINT "Uso:"
      COLOR 15
      PRINT "   listbin nomedoarquivo.bin"
      COLOR 7
      PRINT
      PRINT "LISTBIN.EXE le um arquivo .BIN contendo ";
      PRINT "um programa em BASIC do MC 1000 e"
      PRINT "reproduz o efeito do comando LIST."
      PRINT
      PRINT "Para maiores informacoes, consulte"
      PRINT "http://mc-1000.wikispaces.com/Cassete"
      PRINT
      END
   END IF
END SUB

*/