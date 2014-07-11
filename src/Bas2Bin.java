import java.io.*;
import java.util.*;
import java.util.regex.*;

public class Bas2Bin {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		if (args.length == 0) {
			System.err.println(
					"\nUso:\n\n" +
					"   java Bas2Bin nomedoarquivo.bas\n\n" +
					"Bas2Bin le um arquivo .BAS (ou .TXT) contendo  um programa em BASIC do\n" +
					"microcomputador MC-1000 e o converte em um arquivo .BIN com a sequencia\n" +
					"de bytes correspondente a representacao desse programa em fita cassete.\n\n" +
					"Para maiores informacoes, consulte http://mc-1000.wikispaces.com/Cassete"
			);
			System.exit(0);
		}
		
		String nomeDoArquivoBas = args[0];
		print("Arquivo: [");
		print(nomeDoArquivoBas);
		println("].");
		
		File arquivoBas = new File(nomeDoArquivoBas);
		if (!arquivoBas.exists()) {
			System.err.println(">>>! Arquivo não existe.");
			System.exit(0);
		}
		
		if (arquivoBas.length() == 0) {
			System.err.println(">>>! Arquivo vazio.");
			System.exit(0);
		}
		// BufferedReader para poder ler o arquivo linha a linha.
		FileInputStream fis = new FileInputStream(arquivoBas);
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		
		ArrayList<Integer> listaDeBytes = new ArrayList<Integer>();
		
		int enderecoDeInicio = 0x03d5;
		int enderecoDeFim; // A definir.
		
		// ID do arquivo: cinco espaços + CR.
		// (Arquivo pode ser carregado com comando LOAD sem parâmetros.)
		listaDeBytes.add((int) ' ');
		listaDeBytes.add((int) ' ');
		listaDeBytes.add((int) ' ');
		listaDeBytes.add((int) ' ');
		listaDeBytes.add((int) ' ');
		listaDeBytes.add((int) '\r');
		
		// Endereço de início.
		listaDeBytes.add(enderecoDeInicio & 0xff);
		listaDeBytes.add(enderecoDeInicio >> 8);
		
		// Endereço de fim. (Apenas reserva o espaço).
		int indiceDoEnderecoDeFim = listaDeBytes.size();
		listaDeBytes.add(0);
		listaDeBytes.add(0);
		
		final String[] tokens = new String[] {
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
		final Pattern[] pTokens = new Pattern[tokens.length];
		for (int i = 0; i < tokens.length; i++) {
			pTokens[i] = Pattern.compile("^(" + Pattern.quote(tokens[i]) + ")\\s*(.*)$", Pattern.CASE_INSENSITIVE);
		}
		
		Pattern pLinhaComNumero = Pattern.compile("^\\s*(\\d+)\\s*(.*?)\\s*$");
		Pattern pString = Pattern.compile("^(\"[^\"]*\"?)(.*)$");
		Matcher m;
		
		final int CONTEUDO_NORMAL_DA_LINHA = 0;
		final int CONTEUDO_DE_COMANDO_DATA = 1;
		
		int enderecoDaProximaLinha = enderecoDeInicio;
		String linhaBas;
		
		while((linhaBas = br.readLine()) != null) {
			println(linhaBas);
			m = pLinhaComNumero.matcher(linhaBas);
			if (m.find()) {
				int numeroDaLinha;
				if (m.group(1).length() > 5 || ((numeroDaLinha = Integer.parseInt(m.group(1))) > 65535)) {
					println(">>>! Linha ignorada: Número de linha ausente.");
					break;
				}
				linhaBas = m.group(2);
				StringBuffer linhaBin = new StringBuffer();
				linhaBin.append((char) (numeroDaLinha & 0xff));
				linhaBin.append((char) ((numeroDaLinha >> 8) & 0xff));
				int estado = CONTEUDO_NORMAL_DA_LINHA;
				while (!linhaBas.isEmpty()) {
					blocoSwitch:
					switch (estado) {
					case CONTEUDO_NORMAL_DA_LINHA:
						// Descarta espaços.
						if ((linhaBas = linhaBas.trim()).isEmpty()) {
							break;
						}
						// String?
						m = pString.matcher(linhaBas);
						if (m.find()) {
							linhaBin.append(m.group(1));
							linhaBas = m.group(2);
							break;
						}
						// Token?
						for (int i = 0; i < tokens.length; i++) {
							m = pTokens[i].matcher(linhaBas);
							if (m.find()) {
								linhaBin.append((char) (128 + i));
								linhaBas = m.group(2);
								if (
										m.group(1).equalsIgnoreCase("REM") ||
										m.group(1).equalsIgnoreCase("SAVE") ||
										m.group(1).equalsIgnoreCase("LOAD")
								) {
									// Incorpora todo o resto da linha.
									linhaBin.append(linhaBas);
									linhaBas = "";
								} else if (m.group(1).equalsIgnoreCase("DATA")) {
									estado = CONTEUDO_DE_COMANDO_DATA;
								}
								break blocoSwitch;
							}
						}
						// Caracter comum.
						linhaBin.append(linhaBas.charAt(0));
						linhaBas = linhaBas.substring(1);
						break;
					case CONTEUDO_DE_COMANDO_DATA:
						// String?
						m = pString.matcher(linhaBas);
						if (m.find()) {
							linhaBin.append(m.group(1));
							linhaBas = m.group(2);
							break;
						}
						// Dois-pontos?
						if (linhaBas.charAt(0) == ':') {
							linhaBin.append(':');
							linhaBas = linhaBas.substring(1);
							estado = CONTEUDO_NORMAL_DA_LINHA;
							break;
						}
						// Caracter comum.
						linhaBin.append(linhaBas.charAt(0));
						linhaBas = linhaBas.substring(1);
						break;
					}
				}
				linhaBin.append('\0');
				enderecoDaProximaLinha = enderecoDaProximaLinha + linhaBin.length() + 2;
				listaDeBytes.add(enderecoDaProximaLinha & 0xff);
				listaDeBytes.add((enderecoDaProximaLinha >> 8) & 0xff);
				for (int i = 0; i < linhaBin.toString().length(); i++) {
					listaDeBytes.add((int) linhaBin.charAt(i));
				}
			} else {
				println(">>>! Linha ignorada: Numero de linha ausente.");
			}
		}
		// Fim do programa BASIC.
		// Endereço 0x0000 + byte 0x00.
		listaDeBytes.add(0);
		listaDeBytes.add(0);
		listaDeBytes.add(0);
		enderecoDeFim = enderecoDaProximaLinha + 2;
		listaDeBytes.set(indiceDoEnderecoDeFim, enderecoDeFim & 0xff);
		listaDeBytes.set(indiceDoEnderecoDeFim + 1, (enderecoDeFim >> 8) & 0xff);
		
		Pattern p = Pattern.compile("^(.+)\\.(bas|txt)$", Pattern.CASE_INSENSITIVE);
		m = p.matcher(nomeDoArquivoBas);
		String nomeDoArquivoBin =
			(m.find() ? m.group(1) : nomeDoArquivoBas) +
			".bin";
		print(">>> Salvando para arquivo: [");
		print(nomeDoArquivoBin);
		println("].");
		File arquivoBin = new File(nomeDoArquivoBin);
		
		FileOutputStream fos = new FileOutputStream(arquivoBin);
		byte[] listaDeBytesTemp = new byte[listaDeBytes.size()];
		for (int i = 0; i < listaDeBytes.size(); i++) {
			listaDeBytesTemp[i] = listaDeBytes.get(i).byteValue();
		}
		fos.write(listaDeBytesTemp);
		fos.close();
		println(">>> Arquivo salvo.");
	}
	
	static void print(String s) {
		System.out.print(s);
	}
	
	static void println(String s) {
		System.out.println(s);
	}
	

}

/*
' BAS2BIN.
'
' BAS2BIN.EXE le um arquivo .BAS ou .TXT contendo um programa em BASIC
' e o converte em um arquivo .BIN com a sequencia de bytes correspondente
' `a representacao desse programa na memoria do microcomputador MC 1000.

DECLARE SUB obtemnomesarq (nomearqbas AS STRING, nomearqbin AS STRING)
DECLARE FUNCTION uint% (x AS LONG)

DIM palavrareservada(128 TO 223) AS STRING
DIM nomearqbas AS STRING
DIM nomearqbin AS STRING
DIM nomearq AS STRING * 5
DIM enderecoinicio AS LONG
DIM enderecoaposfim AS LONG
DIM enderecoproxlinha AS LONG
DIM status AS INTEGER
DIM linhabas AS STRING
DIM linhabin AS STRING
DIM numlinha AS LONG
DIM caracter AS STRING * 1
DIM n AS INTEGER
DIM token AS INTEGER
DIM endereco AS INTEGER

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

obtemnomesarq nomearqbas, nomearqbin

OPEN nomearqbas FOR INPUT AS #1

OPEN nomearqbin FOR OUTPUT AS #2
CLOSE #2
OPEN nomearqbin FOR BINARY AS #2

nomearq = "    "
PUT #2, 1, nomearq
caracter = CHR$(13)
PUT #2, 6, caracter
enderecoinicio = &H3D5
endereco = uint(enderecoinicio)
PUT #2, 7, endereco
enderecoaposfim = 0 ' A definir.
endereco = uint(enderecoaposfim)
PUT #2, 9, endereco

enderecoproxlinha = enderecoinicio
DO WHILE NOT EOF(1)
   LINE INPUT #1, linhabas
   linhabas = UCASE$(LTRIM$(RTRIM$(linhabas)))
   PRINT linhabas
   linhabin = ""
   numlinha = 0
   status = 0
   DO WHILE linhabas <> ""
      SELECT CASE status
      CASE 0
         ' Inicio do numero de linha.
         caracter = LEFT$(linhabas, 1)
         IF caracter >= "0" AND caracter <= "9" THEN
            numlinha = VAL(caracter)
            linhabas = MID$(linhabas, 2)
            status = 1
         ELSE
            PRINT ">>> Linha ignorada: Numero de linha ausente."
            EXIT DO
         END IF
      CASE 1
         ' Resto do numero de linha.
         caracter = LEFT$(linhabas, 1)
         IF caracter >= "0" AND caracter <= "9" THEN
            numlinha = numlinha * 10 + VAL(caracter)
            IF numlinha > 65535 THEN
               PRINT ">>> Linha ignorada: Numero de linha superior a 65535."
               EXIT DO
            END IF
            linhabas = MID$(linhabas, 2)
         ELSE
            linhabin = CHR$(numlinha AND 255) + CHR$(INT(numlinha / 256))
            status = 2
         END IF
      CASE 2
         ' Conteudo normal da linha.
         linhabas = LTRIM$(linhabas)
         caracter = LEFT$(linhabas, 1)
         IF caracter = CHR$(34) THEN
            linhabin = linhabin + caracter
            linhabas = MID$(linhabas, 2)
            status = 3
         ELSE
            token = 0
            FOR n = LBOUND(palavrareservada) TO UBOUND(palavrareservada)
               IF LEFT$(linhabas, LEN(palavrareservada(n))) = palavrareservada(n) THEN
                  token = n
                  EXIT FOR
               END IF
            NEXT
            IF token <> 0 THEN
               linhabin = linhabin + CHR$(token)
               linhabas = LTRIM$(MID$(linhabas, LEN(palavrareservada(token)) + 1))
               SELECT CASE palavrareservada(token)
               CASE "REM"
                  status = 4
               CASE "DATA"
                  status = 5
               END SELECT
            ELSE
               linhabin = linhabin + caracter
               linhabas = MID$(linhabas, 2)
            END IF
         END IF
      CASE 3
         ' String.
         caracter = LEFT$(linhabas, 1)
         linhabin = linhabin + caracter
         linhabas = MID$(linhabas, 2)
         IF caracter = CHR$(34) THEN
            status = 2
         END IF
      CASE 4
         ' REM.
         linhabin = linhabin + linhabas
         linhabas = ""
      CASE 5
         ' DATA.
         caracter = LEFT$(linhabas, 1)
         linhabin = linhabin + caracter
         linhabas = MID$(linhabas, 2)
         IF caracter = ":" THEN
            status = 2
         ELSEIF caracter = CHR$(34) THEN
            status = 6
         END IF
      CASE 6
         ' String em DATA.
         caracter = LEFT$(linhabas, 1)
         linhabin = linhabin + caracter
         linhabas = MID$(linhabas, 2)
         IF caracter = CHR$(34) THEN
            status = 5
         END IF
      END SELECT
   LOOP
   IF linhabin <> "" THEN
      linhabin = linhabin + CHR$(0)
      enderecoproxlinha = enderecoproxlinha + LEN(linhabin) + 2
      endereco = uint(enderecoproxlinha)
      PUT #2, LOF(2) + 1, endereco
      PUT #2, LOF(2) + 1, linhabin
   END IF
LOOP
' Fim do programa BASIC.
endereco = uint(0)
PUT #2, LOF(2) + 1, endereco

' Byte extra no final.
linhabin = CHR$(0)
PUT #2, LOF(2) + 1, linhabin

' Completa registro.
enderecoaposfim = enderecoproxlinha + 2
endereco = uint(enderecoaposfim)
PUT #2, 9, endereco

' Fecha arquivos e termina.
CLOSE
END

SUB obtemnomesarq (nomearqbas AS STRING, nomearqbin AS STRING)
   DIM argumentos AS STRING
   DIM posicaoespaco AS INTEGER

   ' Analisa a linha de comando para obter o nome do arquivo .WAV.
   argumentos = LTRIM$(COMMAND$)
   posicaoespaco = INSTR(argumentos, " ")
   IF posicaoespaco = 0 THEN
      nomearqbas = argumentos
   ELSE
      nomearqbas = LEFT$(argumentos, posicaoespaco - 1)
   END IF

   IF (RIGHT$(nomearqbas, 4) = ".BAS" OR RIGHT$(nomearqbas, 4) = ".TXT") AND LEN(nomearqbas) > 4 THEN
      ' Calcula a partir do nome do arquivo .WAV o nome do arquivo
      ' binario de saida, substituindo sua extensao por .BIN.
      nomearqbin = LEFT$(nomearqbas, LEN(nomearqbas) - 4) + ".BIN"

   ELSE
      ' Acusa uso incorreto.
      COLOR 7
      PRINT
      PRINT "Uso:"
      COLOR 15
      PRINT "   bas2bin nomedoarquivo.bas"
      COLOR 7
      PRINT "ou"
      COLOR 15
      PRINT "   bas2bin nomedoarquivo.txt"
      COLOR 7
      PRINT
      PRINT "BAS2BIN.EXE le um arquivo .BAS ou .TXT c";
      PRINT "ontendo um programa em BASIC e o"
      PRINT "converte em um arquivo .BIN com a sequen";
      PRINT "cia de bytes correspondente `a"
      PRINT "representacao desse programa na memoria ";
      PRINT "do microcomputador MC 1000."
      PRINT
      PRINT "Para maiores informacoes, consulte"
      PRINT "http://mc-1000.wikispaces.com/Cassete"
      PRINT
      END
   END IF
END SUB

FUNCTION uint% (x AS LONG)
   uint% = x - (65536 AND x > 32767)
END FUNCTION
*/