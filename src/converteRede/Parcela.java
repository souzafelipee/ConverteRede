package converteRede;

public class Parcela {
	String valor1Parcela;
	String valorDemaisParcelas;
	String valorBrutoParcelas;
	String valorBrutoUltimaParcela;
	String valorTotal;
	int qtdeParcelas;
	int qtdeRestante;
	String nrCartao;
	long nrCV;
	long nrRV;
	String hora;
	
	public void setQtdeParcelas(int qtdeParcelas) {
		this.qtdeParcelas = qtdeParcelas;
		this.qtdeRestante= qtdeParcelas;
	}
	
}
