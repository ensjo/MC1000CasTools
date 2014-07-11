import java.io.*;
import javax.sound.sampled.*;
import java.util.*;
import java.util.regex.*;

public class Bin2Wav {
	
	static int _byte;
	static ArrayList<Byte> listaDeAmostras;

	/**
	 * @param args
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		if (args.length == 0) {
			System.err.println(
					"\nUso:\n\n" +
					"   java Bin2Wav nomedoarquivo.bin\n\n" +
					"Bin2Wav le um arquivo .BIN, e produz um arquivo .WAV\n" +
					"correspondente a esse arquivo salvo em formato sonoro pelo\n" +
					"microcomputador MC-1000 (para armazenamento em fita cassete).\n\n" +
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
		
		// Produz amostras num ArrayList, para poder saber a quantidade.
		listaDeAmostras = new ArrayList<Byte>();
		
		for (int i = 0; i < 4096; i++) {
			pulsoCurto();
		}
		for (int i = 0; i < 256; i++) {
			pulsoLongo();
		}
		
		while((_byte = fis.read()) != -1) {
			// Marca de início de byte.
			pulsoCurto();
			// Os oito bits do byte, do menos para o mais significativo.
			boolean par = true;
			for (int i = 0; i < 8; i++) {
				if ((_byte & (1 << i)) != 0) {
					// Bit 1.
					pulsoCurto();
					par = !par;
				} else {
					// Bit 0.
					pulsoLongo();
				}
			}
			// Bit de paridade.
			if (par) {
				pulsoCurto();
			} else {
				pulsoLongo();
			}
		}
		
		// Agora podemos criar um array de bytes.
		byte[] amostras = new byte[listaDeAmostras.size()];
		for (int i = 0; i < listaDeAmostras.size(); i++) {
			amostras[i] = listaDeAmostras.get(i).byteValue();
		}
		
		Pattern p = Pattern.compile("^(.+)\\.bin$", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(nomeDoArquivoBin);
		String nomeDoArquivoWav =
			(m.find() ? m.group(1) : nomeDoArquivoBin) +
			".wav";
		print(">>> Salvando para arquivo: [");
		print(nomeDoArquivoWav);
		println("]");
		File arquivoWav = new File(nomeDoArquivoWav);
		
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

		AudioSystem.write(fluxoDeEntradaDeAudio, AudioFileFormat.Type.WAVE, arquivoWav);
		
		println(">>> Arquivo salvo.");
	}
	
	static void print(String s) {
		System.out.print(s);
	}
	
	static void println(String s) {
		System.out.println(s);
	}
	
	static void pulsoCurto() {
		amostrasBaixas();
		amostrasAltas();
	}
	
	static void pulsoLongo() {
		amostrasBaixas();
		amostrasBaixas();
		amostrasAltas();
		amostrasAltas();
	}
	
	static void amostrasBaixas() {
		// 4 amostras a 11025 amostras por segundo.
		listaDeAmostras.add(new Byte((byte) -128));
		listaDeAmostras.add(new Byte((byte) -128));
		listaDeAmostras.add(new Byte((byte) -128));
		listaDeAmostras.add(new Byte((byte) -128));
	}
	
	static void amostrasAltas() {
		// 4 amostras a 11025 amostras por segundo.
		listaDeAmostras.add(new Byte((byte) +127));
		listaDeAmostras.add(new Byte((byte) +127));
		listaDeAmostras.add(new Byte((byte) +127));
		listaDeAmostras.add(new Byte((byte) +127));
	}

}

/*
' BIN2WAV.
'
' BIN2WAV.EXE le um arquivo .BIN e produz um arquivo .WAV correspondente
' a esse arquivo salvo em formato sonoro pelo microcomputador MC 1000.

DECLARE SUB obtemnomesarq (nomearqbin AS STRING, nomearqwav AS STRING)
DECLARE SUB abrearqwav (nomearqwav AS STRING, w AS ANY)
DECLARE SUB gravabit (w AS ANY, bit AS INTEGER)
DECLARE SUB gravabyte (w AS ANY, byte AS INTEGER)
DECLARE FUNCTION lebyte% ()

' Tipos definidos para a leitura de arquivo WAV, conforme
' http://ccrma.stanford.edu/courses/422/projects/WaveFormat/

TYPE wavchunk
   id AS STRING * 4
   size AS LONG
   format AS STRING * 4
END TYPE

TYPE wavsubchunk
   id AS STRING * 4
   size AS LONG
END TYPE

TYPE wavfmtsubchunk
   audioformat AS INTEGER
   numchannels AS INTEGER
   samplerate AS LONG
   byterate AS LONG
   blockalign AS INTEGER
   bitspersample AS INTEGER
   extraparamsize AS INTEGER
END TYPE

' Outros tipos.

TYPE wavfile
   c AS wavchunk
   posc AS LONG
   sc AS wavsubchunk
   possc AS LONG
   fsc AS wavfmtsubchunk
   pulsoporamostra AS DOUBLE
   posiniciodasamostras AS LONG
   posfimdasamostras AS LONG
END TYPE

TYPE pulso
   comprimento AS DOUBLE
   curto AS INTEGER
   longo AS INTEGER
   bit0 AS INTEGER
   bit1 AS INTEGER
END TYPE

DIM posleitura AS LONG
DIM nomearqwav AS STRING
DIM nomearqbin AS STRING
DIM w AS wavfile
DIM p AS pulso
DIM n AS INTEGER
DIM byte AS INTEGER
DIM nomearq AS STRING
DIM enderecoinicio AS LONG
DIM enderecoaposfim AS LONG

obtemnomesarq nomearqbin, nomearqwav

' Abre arquivo binario.
OPEN nomearqbin FOR BINARY AS #1
posleitura = 1

abrearqwav nomearqwav, w

' O arquivo sonoro gerado pelo MC 1000 se inicia com uma sequencia de
' 4096 pulsos curtos (bits 1) e 256 pulsos longos (bits 0).
' O MC 1000 le alguns desses pulsos (64 de cada) para reconhecer o
' inicio do arquivo e se ajustar `a velocidade do toca-fitas.
' Para produzir um .WAV mais curto, so emitiremos 512 pulsos curtos.

' Grava uma sequencia de 512 bits 1 (o MC 1000 produz 4096).
FOR n = 1 TO 512
   gravabit w, 1
NEXT n
PRINT ">>> Gravados 512 bits 1."
 
' Grava uma sequencia de 256 bits 0.
FOR n = 1 TO 256
   gravabit w, 0
NEXT n
PRINT ">>> Gravados 256 bits 0."

' Grava o nome do arquivo do MC 1000.
' 14 caracteres ou menos.
' Se menos, termina com CR.
nomearq = ""
FOR n = 1 TO 14
   byte = lebyte
   gravabyte w, byte
   IF byte = 13 THEN EXIT FOR
   nomearq = nomearq + CHR$(byte)
NEXT n
PRINT ">>> Gravado nome do arquivo: '"; nomearq; "'"

' Grava o endereco de inicio do bloco.
byte = lebyte
gravabyte w, byte
enderecoinicio = byte
byte = lebyte
gravabyte w, byte
enderecoinicio = enderecoinicio + 256 * byte
PRINT ">>> Gravado endereco de inicio do bloco: "; RIGHT$("000" + HEX$(enderecoinicio), 4)

' Grava o endereco de fim do bloco.
byte = lebyte
gravabyte w, byte
enderecoaposfim = byte
byte = lebyte
gravabyte w, byte
enderecoaposfim = enderecoaposfim + 256 * byte
PRINT ">>> Gravado endereco seguinte ao fim do bloco: "; RIGHT$("000" + HEX$(enderecoaposfim), 4)

' Grava o bloco de dados propriamente dito.
DO WHILE posleitura <= LOF(1)
   byte = lebyte
   gravabyte w, byte
LOOP
PRINT ">>> Fim do arquivo do MC 1000."

' Atualiza registros incompletos.
w.posfimdasamostras = LOF(2)
w.sc.size = w.posfimdasamostras - w.posiniciodasamostras + 1
PUT #2, w.possc, w.sc
w.c.size = w.posfimdasamostras - 8
PUT #2, w.posc, w.c

' Fecha arquivos e termina.
CLOSE
END

SUB abrearqwav (nomearqwav AS STRING, w AS wavfile)
   OPEN nomearqwav FOR OUTPUT AS #2
   CLOSE #2
   OPEN nomearqwav FOR BINARY AS #2

   w.c.id = "RIFF"
   w.c.size = 0 ' A definir.
   w.c.format = "WAVE"
   w.posc = LOF(2) + 1
   PUT #2, w.posc, w.c

   w.sc.id = "fmt "
   w.sc.size = LEN(w.fsc)
   PUT #2, LOF(2) + 1, w.sc

   w.fsc.audioformat = 1
   w.fsc.numchannels = 1
   w.fsc.samplerate = 2757
   ' Cada pulso curto consome 16 amostras num arquivo
   ' .WAV a 44100 amostras por segundo. Usando 1 amostra
   ' por pulso, a taxa deveria ser 2756,25 amostras por
   ' segundo. Arredondando para baixo o resultado seria
   ' ligeiramente mais lento. Optamos por arredondar para
   ' cima e produzi-lo um pouco mais rápido.
   w.fsc.bitspersample = 8
   w.fsc.blockalign = w.fsc.numchannels * w.fsc.bitspersample / 8
   w.fsc.byterate = w.fsc.samplerate * w.fsc.blockalign
   w.fsc.extraparamsize = 0
   PUT #2, LOF(2) + 1, w.fsc

   w.sc.id = "data"
   w.sc.size = 0 ' A definir.
   w.possc = LOF(2) + 1
   PUT #2, w.possc, w.sc

   w.posiniciodasamostras = LOF(2) + 1
END SUB

SUB gravabit (w AS wavfile, bit AS INTEGER)
   DIM buffer AS STRING

   IF bit = 0 THEN
      buffer = CHR$(255) + CHR$(255) + CHR$(0) + CHR$(0)
   ELSE
      buffer = CHR$(255) + CHR$(0)
   END IF

   PUT #2, LOF(2) + 1, buffer
END SUB

SUB gravabyte (w AS wavfile, byte AS INTEGER)
   DIM n AS INTEGER
   DIM paridadepar AS INTEGER
   
   ' Marca de inicio de byte.
   gravabit w, 1

   ' Grava os oito bits, do menos ao mais significativo.
   paridadepar = NOT 0
   FOR n = 0 TO 7
      IF byte AND (2 ^ n) THEN
         gravabit w, 1
         paridadepar = NOT paridadepar
      ELSE
         gravabit w, 0
      END IF
   NEXT

   ' Grava um bit de paridade (1 = par, 0 = impar).
   gravabit w, 1 AND paridadepar
END SUB

FUNCTION lebyte%
   SHARED posleitura AS LONG
   DIM byte AS STRING * 1

   IF posleitura > LOF(1) THEN
      PRINT ">>> Fim inesperado do arquivo .BIN."
      END
   ELSE
      GET #1, posleitura, byte
      posleitura = posleitura + 1
      lebyte = ASC(byte)
   END IF
END FUNCTION

SUB obtemnomesarq (nomearqbin AS STRING, nomearqwav AS STRING)
   DIM argumentos AS STRING
   DIM posicaoespaco AS INTEGER

   ' Analisa a linha de comando para obter o nome do arquivo .BIN.
   argumentos = LTRIM$(COMMAND$)
   posicaoespaco = INSTR(argumentos, " ")
   IF posicaoespaco = 0 THEN
      nomearqbin = argumentos
   ELSE
      nomearqbin = LEFT$(argumentos, posicaoespaco - 1)
   END IF

   IF RIGHT$(nomearqbin, 4) = ".BIN" AND LEN(nomearqbin) > 4 THEN
      ' Calcula a partir do nome do arquivo .WAV o nome do arquivo
      ' binario de saida, substituindo sua extensao por .BIN.
      nomearqwav = LEFT$(nomearqbin, LEN(nomearqbin) - 4) + ".WAV"

   ELSE
      ' Acusa uso incorreto.
      COLOR 7
      PRINT
      PRINT "Uso:"
      COLOR 15
      PRINT "   bin2wav nomedoarquivo.bin"
      COLOR 7
      PRINT
      PRINT "BIN2WAV.EXE le um arquivo .BIN, e produz";
      PRINT " um arquivo .WAV correspondente a esse"
      PRINT "arquivo salvo em formato sonoro pelo mic";
      PRINT "rocomputador MC 1000 (para armazenamento";
      PRINT "em fita cassete)."
      PRINT
      PRINT "Para maiores informacoes, consulte"
      PRINT "http://mc-1000.wikispaces.com/Cassete"
      PRINT
      END
   END IF
END SUB
*/