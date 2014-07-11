import java.io.*;
import javax.sound.sampled.*;
import java.util.regex.*;

public class Wav2Bin {
	
	static String nomeDoArquivoWav;

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException, UnsupportedAudioFileException {
		if (args.length == 0) {
			System.err.println(
					"\n" +
					"Uso:\n\n" +
					"   java Wav2Bin nomedoarquivo.wav\n\n" +
					"Wav2Bin lê um arquivo .WAV, identifica nele um arquivo salvo\n" +
					"em formato sonoro pelo microcomputador MC-1000 (para armazenamento\n" +
					"em fita cassete) e extrai os bytes assim codificados em um arquivo\n" +
					".BIN.\n\n" +
					"Para acelerar o processamento, a impressao do progresso da analise\n" +
					"pode ser redirecionada da tela para um arquivo adicionando-se:\n\n" +
					"   > arquivo.txt\n\n" +
					"apos o comando; ou ser totalmente suprimida adicionando-se:\n\n" +
					"   > nul\n\n" +
					"Para maiores informacoes, consulte http://mc-1000.wikispaces.com/Cassete"
			);
			System.exit(0);
		}
		nomeDoArquivoWav = args[0];
		print(">>> Arquivo: [");
		print(nomeDoArquivoWav);
		println("].");

		// Código adaptado de
		// http://stackoverflow.com/questions/938304/how-to-get-audio-data-from-a-mp3

		File arquivoWav = new File(nomeDoArquivoWav);
		if (!arquivoWav.exists()) {
			System.err.println(">>>! Arquivo não existe.");
			System.exit(0);
		}
		AudioInputStream fluxoDeEntrada = null;
		fluxoDeEntrada = AudioSystem.getAudioInputStream(arquivoWav);
		AudioFormat formatoBase = fluxoDeEntrada.getFormat();
		print(">>> ");
		println(formatoBase.toString());

		AudioInputStream fluxoDeEntradaDecodificado = null;
		AudioFormat formatoDecodificado = null;

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
			print(">>> ");
			println(formatoDecodificado.toString());
			fluxoDeEntradaDecodificado = AudioSystem.getAudioInputStream(formatoDecodificado, fluxoDeEntrada);
		}
		
		int tamanhoDaAmostraEmBytes = formatoDecodificado.getSampleSizeInBits() / 8;
		int tamanhoDoQuadroEmBytes = formatoDecodificado.getChannels() * tamanhoDaAmostraEmBytes;
		
		byte[] amostras = new byte[(int) fluxoDeEntradaDecodificado.getFrameLength()];
		int indiceAmostras = 0;
		byte amostraMax = 0;
		byte amostraMin = 0;
		
		int numBytesRead = 0;
		byte[] audioBytes = new byte[tamanhoDoQuadroEmBytes];
		while ((numBytesRead = fluxoDeEntradaDecodificado.read(audioBytes)) != -1) {
			amostra = 0;
			
			if (tamanhoDaAmostraEmBytes == 1) {
				amostra = audioBytes[0];
			} else {
				amostra = audioBytes[formatoDecodificado.isBigEndian() ? 0 : (tamanhoDaAmostraEmBytes - 1)];
			}
			if (formatoDecodificado.getEncoding() == AudioFormat.Encoding.PCM_UNSIGNED) {
				amostra = (byte) ((amostra & 255) - 128);
			}
			
			amostras[indiceAmostras++] = amostra;
			amostraMax = (byte) Math.max(amostra, amostraMax);
			amostraMin = (byte) Math.min(amostra, amostraMin);
			
			if (numBytesRead == 0) System.exit(0);
		}
		
		println(">>> Análise concluída.");
		println(">>> Quantidade de frames: " + fluxoDeEntradaDecodificado.getFrameLength() + ".");
		println(">>> Amostras obtidas: " + indiceAmostras + ".");
		println(">>> Mínimo: " + amostraMin + ".");
		println(">>> Máximo: " + amostraMax + ".");
		
		duracaoDaAmostra = 1 / formatoDecodificado.getFrameRate();
		
		trataInicioDasAmostras();
		for (indiceAmostras = 0; indiceAmostras < amostras.length; indiceAmostras++) {
			amostra = amostras[indiceAmostras];
			
			// Normaliza:
			amostra = (byte) ((amostra - amostraMin) * 255 / (amostraMax - amostraMin) - 128);
			amostras[indiceAmostras] = amostra;
			
			print("Amostra " + indiceAmostras + ":");
			int pos = (amostra + 128) * 50 / 255;
			for (int i = 0; i < pos; i++) System.out.print(" ");
			println("# " + amostra);
			
			trataAmostra();
		}
		trataFimDasAmostras();
	}
	
	static void print(String s) {
		System.out.print(s);
	}
	
	static void println(String s) {
		System.out.println(s);
	}
	
	static final int PROCURANDO_64_PULSOS_CURTOS = 0;
	static final int LENDO_64_PULSOS_CURTOS = 1;
	static final int PROCURANDO_64_PULSOS_LONGOS = 2;
	static final int LENDO_64_PULSOS_LONGOS = 3;
	static final int PROCURANDO_INICIO_DO_BYTE = 4;
	static final int LENDO_INICIO_DO_BYTE = 5;
	static final int LENDO_BYTE = 6;
	static final int LENDO_PARIDADE = 7;
	
	static double duracaoDoPulso;
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
	
	static int estado;
	static double duracaoDaAmostra;
	static byte amostra;
	static int sinalDaAmostra;
	static byte valorDoPulso;
	static int contador;
	static int _byte;
	static boolean par;
	static int contadorDeArquivos;
	
	static void trataInicioDasAmostras() {
		estado = PROCURANDO_64_PULSOS_CURTOS;
		sinalDaAmostra = -1;
		contadorDeArquivos = 0;
	}
	
	static void trataAmostra() throws IOException {
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
	
	static void trataFimDasAmostras() throws IOException {
		if (sinalDaAmostra == +1) {
			emitePulso();
		}
		trataFimDosPulsos();
	}
	
	static void iniciaPulso() {
		sinalDaAmostra = +1;
		duracaoDoPulso = duracaoDaAmostra;
	}
	
	static void emitePulso() throws IOException {
		if (duracaoDoPulso < duracaoMinDoPulsoCurto) {
			valorDoPulso = 2;
		} else if (duracaoDoPulso < duracaoMaxDoPulsoCurto) {
			valorDoPulso = 1;
		} else if (duracaoDoPulso < duracaoMaxDoPulsoLongo) {
			valorDoPulso = 0;
		} else {
			valorDoPulso = -1;
		}
		trataPulso();
		sinalDaAmostra = -1;
	}
	
	static void trataPulso() throws IOException {
		switch (estado) {
		case PROCURANDO_64_PULSOS_CURTOS:
			if (valorDoPulso == 1) {
				contador = 1;
				estado = LENDO_64_PULSOS_CURTOS;
			}
			break;
		case LENDO_64_PULSOS_CURTOS:
			if (valorDoPulso == 1) {
				if (++contador == 64) {
					println(">>> Lidos 64 pulsos curtos.");
					estado = PROCURANDO_64_PULSOS_LONGOS;
				}
			} else {
				// Pulso estranho na sequencia.
				// Reinicia a contagem.
				estado = PROCURANDO_64_PULSOS_CURTOS;
			}
			break;
		case PROCURANDO_64_PULSOS_LONGOS:
			if (valorDoPulso == 0) {
				contador = 1;
				estado = LENDO_64_PULSOS_LONGOS;
			}
			break;
		case LENDO_64_PULSOS_LONGOS:
			if (valorDoPulso == 0) {
				if (++contador == 64) {
					println(">>> Lidos 64 pulsos longos.");
					estado = PROCURANDO_INICIO_DO_BYTE;
				}
			} else {
				// Pulso estranho na sequencia.
				// Reinicia a contagem.
				estado = PROCURANDO_64_PULSOS_LONGOS;
			}
			break;
		case PROCURANDO_INICIO_DO_BYTE:
			if (valorDoPulso == 1) {
				iniciaByte();
			}
			break;
		case LENDO_INICIO_DO_BYTE:
			if (valorDoPulso < 1) {
				println(">>>! Pulso de início de byte curto demais!");
			} else if (valorDoPulso > 1) {
				println(">>>! Pulso de início de byte longo demais!"); 
			}
			iniciaByte();
			break;
		case LENDO_BYTE:
			switch (valorDoPulso) {
			case -1:
				println(">>>! Pulso de bit longo demais!");
			case 0:
				break;
			case 2:
				println(">>>! Pulso de bit curto demais!");
			case 1:
				_byte |= 1 << contador;
				par = !par;
				break;
			}
			if (++contador == 8) {
				estado = LENDO_PARIDADE;
			}
			break;
		case LENDO_PARIDADE:
			switch (valorDoPulso) {
			case -1:
				println(">>>! Pulso de bit de paridade longo demais!");
			case 0:
				if (par) {
					println(">>>! Erro de paridade.");
				}
				break;
			case 2:
				println(">>>! Pulso de bit de paridade curto demais!");
			case 1:
				if (!par) {
					println(">>>! Erro de paridade.");
				}
			}
			emiteByte();
		}
	}
	
	static void trataFimDosPulsos() throws IOException {
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
	static final int LENDO_ID_DO_ARQUIVO = 1;
	static final int LENDO_ENDERECO_DE_INICIO = 2;
	static final int LENDO_ENDERECO_DE_FIM = 3;
	static final int LENDO_ARQUIVO = 4;
	
	static int estado2;
	static int[] idDoArquivo;
	static int tamanhoDoIdDoArquivo;
	static int enderecoDeInicio;
	static int enderecoDeFim;
	static int tamanhoDoArquivo;
	static int[] bytesDoArquivo;
	static long contadorDeBytes;
	
	static void trataInicioDosBytes() {}
	
	static void iniciaByte() {
		estado = LENDO_BYTE;
		contador = 0;
		_byte = 0;
		par = true;
	}
	
	static void emiteByte() throws IOException {
		print(">>> Byte: ");
		println(Integer.toHexString(_byte));
		trataByte();
		if (estado2 == PROCURANDO_ARQUIVO) {
			estado = PROCURANDO_64_PULSOS_CURTOS;
		} else {
			estado = LENDO_INICIO_DO_BYTE;
		}
	}
	
	static void trataFimDosBytes() throws IOException {
		if (estado2 == LENDO_ARQUIVO) {
			emiteArquivo();
		}
		trataFimDosArquivos();
	}
	
	static void iniciaArquivo() throws IOException {
		idDoArquivo = new int[14];
		tamanhoDoIdDoArquivo = 0;
		enderecoDeInicio = 0x0000;
		enderecoDeFim = 0x0000;
		tamanhoDoArquivo = 0;
		bytesDoArquivo = null;
		contadorDeBytes = 0;
		estado2 = LENDO_ID_DO_ARQUIVO;
		trataByte();
	}
	
	static void trataByte() throws IOException {
		switch (estado2) {
		case PROCURANDO_ARQUIVO:
			iniciaArquivo();
			break;
		case LENDO_ID_DO_ARQUIVO:
			idDoArquivo[(int) contadorDeBytes++] = _byte;
			if (contadorDeBytes == 14 || _byte == '\r') {
				tamanhoDoIdDoArquivo = ((int) contadorDeBytes) - (_byte == '\r' ? 1 : 0);
				print(">>> ID do arquivo: [");
				for (int i = 0; i < tamanhoDoIdDoArquivo; i++) {
					print(new String(new char[] { (char) idDoArquivo[i] } ));
				}
				println("]");
				estado2 = LENDO_ENDERECO_DE_INICIO;
				contadorDeBytes = 0;
			}
			break;
		case LENDO_ENDERECO_DE_INICIO:
			enderecoDeInicio |= (_byte << contadorDeBytes);
			if (contadorDeBytes == 0) {
				contadorDeBytes = 8;
			} else {
				print(">>> Endereço de início: ");
				println(Integer.toHexString(enderecoDeInicio));
				contadorDeBytes = 0;
				estado2 = LENDO_ENDERECO_DE_FIM;
			}
			break;
		case LENDO_ENDERECO_DE_FIM:
			enderecoDeFim |= (_byte << contadorDeBytes);
			if (contadorDeBytes == 0) {
				contadorDeBytes = 8;
			} else {
				print(">>> Endereço de fim: ");
				println(Integer.toHexString(enderecoDeFim));
				tamanhoDoArquivo = (enderecoDeFim - enderecoDeInicio) + 1;
				bytesDoArquivo = new int[tamanhoDoArquivo];
				contadorDeBytes = 0;
				estado2 = LENDO_ARQUIVO;
			}
			break;
		case LENDO_ARQUIVO:
			bytesDoArquivo[(int) contadorDeBytes++] = _byte;
			if (contadorDeBytes == tamanhoDoArquivo) {
				emiteArquivo();
			}
			break;
		}
	}

	static void emiteArquivo() throws IOException {
		println(">>> Fim de arquivo.");
		trataArquivo();
		contadorDeArquivos++;
		estado2 = PROCURANDO_ARQUIVO;
	}
	
	static void trataArquivo() throws IOException {
		Pattern p = Pattern.compile("^(.+)\\.\\w+$", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(nomeDoArquivoWav);
		String nomeDoArquivoBin =
			(m.find() ? m.group(1) : nomeDoArquivoWav) +
			(contadorDeArquivos == 0 ? "" : ("_" + contadorDeArquivos)) +
			".bin";
		print(">>> Salvando para arquivo: [");
		print(nomeDoArquivoBin);
		println("].");
		File arquivoBin = new File(nomeDoArquivoBin);
		FileOutputStream fos = new FileOutputStream(arquivoBin);
		for (int i = 0; i < tamanhoDoIdDoArquivo; i++) {
			fos.write(idDoArquivo[i]);
		}
		if (tamanhoDoIdDoArquivo < 14) {
			fos.write('\r');
		}
		fos.write(enderecoDeInicio & 0xff);
		fos.write((enderecoDeInicio >> 8) & 0xff);
		fos.write(enderecoDeFim & 0xff);
		fos.write((enderecoDeFim >> 8) & 0xff);
		for (int i = 0; i < bytesDoArquivo.length; i++) {
			fos.write(bytesDoArquivo[i]);
		}
		fos.close();
		println(">>> Arquivo salvo.");
	}
	
	static void trataFimDosArquivos() {}

}

/*

' WAV2BIN.
'
' WAV2BIN.EXE le um arquivo .WAV, identifica nele um arquivo salvo em formato
' sonoro pelo microcomputador MC 1000 (para armazenamento em fita cassete) e
' extrai os bytes assim codificados em um arquivo .BIN.

DECLARE SUB obtemnomesarq (nomearqwav AS STRING, nomearqbin AS STRING)
DECLARE SUB abrearqwav (nomearqwav AS STRING, w AS ANY)
DECLARE SUB lepulso (w AS ANY, p AS ANY)
DECLARE FUNCTION fimdasamostras% (w AS ANY)
DECLARE FUNCTION leamostra% (w AS ANY)
DECLARE FUNCTION lebyte% (w AS ANY)

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
   posleitura AS LONG
   c AS wavchunk
   sc AS wavsubchunk
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

DIM nomearqwav AS STRING
DIM nomearqbin AS STRING
DIM nomearqtxt AS STRING
DIM w AS wavfile
DIM p AS pulso
DIM n AS INTEGER
DIM byte AS INTEGER
DIM nomearq AS STRING
DIM enderecoinicio AS LONG
DIM enderecoaposfim AS LONG

obtemnomesarq nomearqwav, nomearqbin
abrearqwav nomearqwav, w

' O arquivo sonoro gerado pelo MC 1000 se inicia com uma sequencia de
' 512 pulsos curtos (bits 1) e 256 pulsos longos (bits 0).
' O MC 1000 le alguns desses pulsos (64 de cada) para reconhecer o
' inicio do arquivo e se ajustar `a velocidade do toca-fitas.
' Aqui usamo-los apenas para reconhecer o inicio do arquivo.
' Nenhum ajuste e' feito.

' Le uma sequencia de 64 bits 1.
FOR n = 1 TO 64
   IF fimdasamostras(w) THEN END
   lepulso w, p
   IF NOT p.bit1 THEN n = 0 ' Reinicia contagem.
NEXT n
PRINT ">>> Lidos 64 bits 1."
 
' Le uma sequencia de 64 bits 0.
FOR n = 1 TO 64
   IF fimdasamostras(w) THEN END
   lepulso w, p
   IF NOT p.bit0 THEN n = 0 ' Reinicia contagem.
NEXT n
PRINT ">>> Lidos 64 bits 0."

' Apaga o conteudo do arquivo binario, se ja existir.
OPEN nomearqbin FOR OUTPUT AS #2
CLOSE #2

' Abre arquivo binario.
OPEN nomearqbin FOR BINARY AS #2

' Le o nome do arquivo do MC 1000.
' 14 caracteres ou menos.
' Se menos, termina com CR.
nomearq = ""
FOR n = 1 TO 14
   byte = lebyte(w)
   IF byte = 13 THEN EXIT FOR
   nomearq = nomearq + CHR$(byte)
NEXT
PRINT ">>> Lido nome do arquivo: '"; nomearq; "'"

' Le o endereco de inicio do bloco.
enderecoinicio = lebyte(w) + 256 * lebyte(w)
PRINT ">>> Lido endereco de inicio do bloco: "; RIGHT$("000" + HEX$(enderecoinicio), 4)

' Le o endereco de fim do bloco.
enderecoaposfim = lebyte(w) + 256 * lebyte(w)
PRINT ">>> Lido endereco seguinte ao fim do bloco: "; RIGHT$("000" + HEX$(enderecoaposfim), 4)

' Le o bloco de dados propriamente dito.
FOR n = 1 TO enderecoaposfim - enderecoinicio + 1
   byte = lebyte(w)
NEXT
PRINT ">>> Fim do arquivo do MC 1000."

' Le o restante do arquivo, sem processar nada.
DO WHILE NOT fimdasamostras(w)
   n = leamostra(w)
LOOP

' Fecha arquivos e termina.
CLOSE
END

' Le as porcoes do arquivo WAV e sua formatacao interna.
SUB abrearqwav (nomearqwav AS STRING, w AS wavfile)
   OPEN nomearqwav FOR BINARY AS #1

   w.posleitura = 1
   GET #1, w.posleitura, w.c
   w.posleitura = w.posleitura + LEN(w.c)

   IF w.c.id <> "RIFF" AND w.c.format <> "WAVE" THEN
      PRINT ">>> Arquivo nao parece ser do tipo WAV."
      END
   END IF

   PRINT ">>> Arquivo: "; nomarqwav
   PRINT ">>> Tamanho: "; w.c.size

   DO WHILE w.posleitura <= w.c.size
      GET #1, w.posleitura, w.sc
      w.posleitura = w.posleitura + LEN(w.sc)

      SELECT CASE w.sc.id
      CASE "fmt "
         GET #1, w.posleitura, w.fsc
         w.posleitura = w.posleitura + w.sc.size

         PRINT ">>> AudioFormat: "; w.fsc.audioformat
         PRINT ">>> NumChannels: "; w.fsc.numchannels
         PRINT ">>> SampleRate: "; w.fsc.samplerate
         PRINT ">>> ByteRate: "; w.fsc.byterate
         PRINT ">>> BlockAlign: "; w.fsc.blockalign
         PRINT ">>> BitsPerSample: "; w.fsc.bitspersample

         w.pulsoporamostra = 44100# / 16# / w.fsc.samplerate
         PRINT ">>> pulsoporamostra: "; w.pulsoporamostra

      CASE "data"
         w.posiniciodasamostras = w.posleitura
         w.posleitura = w.posleitura + w.sc.size
         w.posfimdasamostras = w.posleitura - 1

      CASE ELSE
         w.posleitura = w.posleitura + w.sc.size

      END SELECT
   LOOP

   w.posleitura = w.posiniciodasamostras
END SUB

FUNCTION fimdasamostras% (w AS wavfile)
   fimdasamostras% = (w.posleitura > w.posfimdasamostras)
END FUNCTION

' Le o canal 1 da amostra atual e avanca para a amostra
' seguinte, ignorando os demais canais.
FUNCTION leamostra% (w AS wavfile)
   DIM word AS INTEGER
   DIM byte AS STRING * 1

   IF NOT fimdasamostras(w) THEN
      SELECT CASE w.fsc.bitspersample
      CASE 8
         GET #1, w.posleitura, byte
         word = (ASC(byte) - 128) * 256
      CASE 16
         GET #1, w.posleitura, word
      END SELECT
      PRINT TAB(25 + word * 25! / 32768); "#"; TAB(51); word; "@"; w.posleitura
      leamostra% = word
      w.posleitura = w.posleitura + w.fsc.blockalign
   ELSE
      leamostra% = 0
   END IF
END FUNCTION

' Le um bit 1 (marca de inicio),
' seguido pelos 8 bits do byte
' (do menos ao mais significativo)
' e depois um bit de paridade.
FUNCTION lebyte% (w AS wavfile)
   DIM p AS pulso
   DIM byte AS INTEGER
   DIM caracter AS STRING * 1
   DIM bit AS INTEGER
   DIM paridadepar AS INTEGER
   DIM corrbyte AS STRING

   byte = 0
   paridadepar = NOT 0

   DO
      IF fimdasamostras(w) THEN END
      lepulso w, p
   LOOP UNTIL p.bit1
   PRINT ">>> Lida marca de inicio de byte."

   IF fimdasamostras(w) THEN END
   FOR bit = 0 TO 7
      IF fimdasamostras(w) THEN EXIT FOR
      lepulso w, p
      IF p.curto THEN
         byte = byte + 2 ^ bit
         paridadepar = NOT paridadepar
      END IF
   NEXT
   PRINT ">>> Lido byte: "; RIGHT$("0" + HEX$(byte), 2)
   lebyte% = byte

   IF fimdasamostras(w) THEN END
   lepulso w, p
   PRINT ">>> Lido bit de paridade."
   IF (paridadepar <> p.curto) THEN
      ' Permite que o usuario corrija o byte.
      PLAY "L32 O0 D F"
      PRINT ">>> Erro de paridade."
      PRINT ">>> 'REBOBINE A FITA E REAPERTE O BOTAO PLAY.'"
      PRINT ">>> Corrigir byte " + RIGHT$("0" + HEX$(byte), 2) + " para (ENTER = aceitar atual)";
      INPUT corrbyte
      IF corrbyte <> "" THEN lebyte% = VAL("&H" + corrbyte)
   END IF

   caracter = CHR$(byte)
   PUT #2, LOF(2) + 1, caracter
END FUNCTION

' Salta um pulso baixo (se houver)
' e le um pulso alto do arquivo wave,
' anotando seu comprimento.
SUB lepulso (w AS wavfile, p AS pulso)
   ' O valor do comprimento e proporcional a duracao
   ' de um pulso curto (aprox. 3,628*10^-4 segundos):
   ' 1 = a duracao de um pulso curto (bit 1).
   ' 2 = a duracao de um pulso longo (o dobro do curto) (bit 0).

   ' As constantes definidas abaixo determinam
   ' tolerancias para reconhecer bits 1 e 0 no
   ' momento da leitura:
   ' De 0,7 a 1,4 = pulso curto (bit 1).
   ' De 1,4 a 2,8 = pulso longo (bit 0).

   CONST limiteinferior = .7071067811865476# ' 2^-0.5
   CONST limiteinterno = 1.414213562373095# ' 2^0.5
   CONST limitesuperior = 2.82842712474619# ' 2^1.5

   DIM amostra AS INTEGER

   p.comprimento = 0

   ' Salta pulso baixo.
   DO WHILE NOT fimdasamostras(w)
      amostra = leamostra(w)
      IF amostra > -2048 THEN
         p.comprimento = w.pulsoporamostra
         EXIT DO
      END IF
   LOOP

   ' Le pulso alto.
   DO WHILE NOT fimdasamostras(w)
      amostra = leamostra(w)
      IF amostra > -2048 THEN
         p.comprimento = p.comprimento + w.pulsoporamostra
      ELSE
         EXIT DO
      END IF
   LOOP

   p.curto = p.comprimento < limiteinterno
   p.longo = NOT p.curto
   p.bit0 = p.longo AND (p.comprimento < limitesuperior)
   p.bit1 = p.curto AND (p.comprimento >= limiteinferior)
END SUB

SUB obtemnomesarq (nomearqwav AS STRING, nomearqbin AS STRING)
   DIM argumentos AS STRING
   DIM posicaoespaco AS INTEGER

   ' Analisa a linha de comando para obter o nome do arquivo .WAV.
   argumentos = LTRIM$(COMMAND$)
   posicaoespaco = INSTR(argumentos, " ")
   IF posicaoespaco = 0 THEN
      nomearqwav = argumentos
   ELSE
      nomearqwav = LEFT$(argumentos, posicaoespaco - 1)
   END IF

   IF RIGHT$(nomearqwav, 4) = ".WAV" AND LEN(nomearqwav) > 4 THEN
      ' Calcula a partir do nome do arquivo .WAV o nome do arquivo
      ' binario de saida, substituindo sua extensao por .BIN.
      nomearqbin = LEFT$(nomearqwav, LEN(nomearqwav) - 4) + ".BIN"

   ELSE
      ' Acusa uso incorreto.
      COLOR 7
      PRINT
      PRINT "Uso:"
      COLOR 15
      PRINT "   wav2bin nomedoarquivo.wav"
      COLOR 7
      PRINT
      PRINT "WAV2BIN.EXE le um arquivo .WAV, identifi";
      PRINT "ca nele um arquivo salvo em formato"
      PRINT "sonoro pelo microcomputador MC 1000 (par";
      PRINT "a armazenamento em fita cassete) e"
      PRINT "extrai os bytes assim codificados em um ";
      PRINT "arquivo .BIN."
      PRINT
      PRINT "Para acelerar o processamento, a impress";
      PRINT "ao do progresso da analise pode ser"
      PRINT "redirecionada da tela para um arquivo ad";
      PRINT "icionando-se:"
      COLOR 15
      PRINT "   > arquivo.txt"
      COLOR 7
      PRINT "apos o comando; ou ser totalmente suprim";
      PRINT "ida adicionando-se:"
      COLOR 15
      PRINT "   > nul"
      COLOR 7
      PRINT
      PRINT "Para maiores informacoes, consulte"
      PRINT "http://mc-1000.wikispaces.com/Cassete"
      PRINT
      END
   END IF
END SUB
*/