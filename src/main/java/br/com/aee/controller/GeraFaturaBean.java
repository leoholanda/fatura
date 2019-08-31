package br.com.aee.controller;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import br.com.aee.model.Beneficiario;
import br.com.aee.model.Dependente;
import br.com.aee.model.Fatura;
import br.com.aee.model.ItemDaFatura;
import br.com.aee.model.MesFatura;
import br.com.aee.model.Plano;
import br.com.aee.repository.DependenteRepository;
import br.com.aee.repository.FaturaRepository;
import br.com.aee.repository.ItemDaFaturaRepository;
import br.com.aee.repository.MesFaturaRepository;
import br.com.aee.repository.PlanoRepository;

@Named("geraFaturaBeanMB")
@ViewScoped
public class GeraFaturaBean implements Serializable {

	private static final long serialVersionUID = -8119249261777502911L;

	@Inject
	private FaturaRepository repository;

	@Inject
	private ItemDaFaturaRepository itemDaFaturaRepository;

	@Inject
	private MesFaturaRepository mesFaturaRepository;

	@Inject
	private PlanoRepository planoRepository;

	@Inject
	private DependenteRepository dependenteRepository;

	private MesFatura mesFatura;

	private Fatura fatura;

	private ItemDaFatura item;
	private String nome = "Associação dos Empregados da Embrapa Roraima";

	@PostConstruct
	public void init() {
		System.out.println(">>> geraFaturaBeanMB()");
		item = new ItemDaFatura();
		fatura = new Fatura();
		mesFatura = new MesFatura();

		this.geraFatura();
	}

	public void geraFatura() {
		if (diaDoMes() >= 1 && diaDoMes() <= 5) {
			if (isFaturaParaEsseMes()) {
				mesFatura.setEvento("Fatura");
				mesFaturaRepository.save(mesFatura);

				System.out.println(">>>>> Gerando Fatura...");

				for (Plano plano : planoRepository.findByPlanoBeneficiarioAtivado()) {
					this.geraFaturaIndividual(plano.getBeneficiario());
				}
			}
		}
	}

	/**
	 * Gera fatura individual
	 */
	public void geraFaturaIndividual(Beneficiario beneficiario) {
		try {
			if (!planoRepository.findByPlanoBeneficiario(beneficiario).isEmpty()) {
				for (Plano plano : planoRepository.findByPlanoBeneficiario(beneficiario)) {
					System.out.println(">>>>> Gerando fatura individual para: " + beneficiario.getNome());
					Double mensalidade = 0.00;

					// TODO calcula mensalidade
					calculaMensalidade(plano, mensalidade);

					// TODO verifica se há servicos adicionais
					servicosAdicionais(plano.getBeneficiario());

					// TODO tras o residuo da fatura anterior e soma
					aplicaResiduoNaFatura(plano);
					fatura.setResiduoDescontado(aplicaResiduoNaFatura(plano));

					// TODO calcula valor do plano de saude
					if (plano.getBeneficiario().getTemPlanoDeSaude()) {
						this.geraFaturaComPlanoDeSaude(plano);
						System.out.println(">>>>> Fatura Com Plano de Saude");

						// TODO se nao tiver plano de saude gera somente mensalidade
					} else {
						this.geraFaturaSemPlanoDeSaude(plano);
						System.out.println(">>>>> Fatura Sem Plano de Saude");
					}

					// TODO pega a data de ontem para setar os juros em caso de atraso
					Calendar ontem = Calendar.getInstance();
					ontem.add(Calendar.DAY_OF_MONTH, -1);

					fatura.setPlano(plano);
					fatura.setDataPagamento(null);
					fatura.setDataJuros(ontem);

					this.dataDeVencimentoFaturaIndividual();

					repository.save(fatura);

					this.geraItensDaFatura(beneficiario);

					this.aplicaMultaPorAtraso(fatura);
					this.aplicaJurosAoDia(fatura);

					fatura = new Fatura();
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	/**
	 * Gera mensalidade para beneficiario
	 *
	 * @param plano
	 * @param mensalidade
	 */
	public void calculaMensalidade(Plano plano, Double mensalidade) {
		mensalidade = 0.00;
		if (plano.getBeneficiario().getSalario() != null) {

			// Gera mensalidade para servidor
			if (plano.getBeneficiario().isBeneficiarioServidor()) {

				if (plano.getBeneficiario().isConsignado()) {
					fatura.setValorMensalidade(mensalidade);

					// Calcula mensalidade para não consignado
				} else {
					// Mensalidade é 0,8% do salário
					mensalidade = plano.getBeneficiario().getSalario() * 0.008;
					fatura.setValorMensalidade(mensalidade);
				}
			} else {
				mensalidade = 70.00;
				fatura.setValorMensalidade(mensalidade);
			}

		} else {
			fatura.setValorMensalidade(mensalidade);
		}
	}

	/**
	 * Calcula o plano de saude
	 * 
	 * @param plano
	 */
	public void geraFaturaComPlanoDeSaude(Plano plano) {

		// TODO Verifica se o plano do beneficiario é enfermaria
		if (plano.getBeneficiario().isEnfermaria()) {
			Double valorEnfermaria = this
					.valorAcomodacao(plano.getBeneficiario().getFaixaEtaria().getValorEnfermaria());

			// TODO Se o beneficiario tiver dependente soma todos
			Double valorDependente = this.valorDependente(plano.getBeneficiario());
			fatura.setValorPlanoDeSaude(valorEnfermaria + valorDependente);

		} else {
			Double valorApartamento = this
					.valorAcomodacao(plano.getBeneficiario().getFaixaEtaria().getValorApartamento());

			// TODO Se o beneficiario tiver dependente soma todos
			Double valorDependente = this.valorDependente(plano.getBeneficiario());
			fatura.setValorPlanoDeSaude(valorApartamento + valorDependente);

		}

		fatura.setValorTotalGerado(
				(fatura.getValorPlanoDeSaude() + fatura.getValorMensalidade() + fatura.getValorServicosAdicionais()));

		fatura.setValorTotal((fatura.getValorPlanoDeSaude() + fatura.getValorMensalidade()
				+ fatura.getResiduoDescontado() + fatura.getValorServicosAdicionais()));
	}

	/**
	 * Gera fatura somente com a taxa da mensalidade(sem plano de saude)
	 * 
	 * @param plano
	 */
	public void geraFaturaSemPlanoDeSaude(Plano plano) {
		fatura.setValorPlanoDeSaude(0.00);
		fatura.setValorTotalGerado((fatura.getValorMensalidade()) + fatura.getValorServicosAdicionais());
		fatura.setValorTotal(
				(fatura.getValorMensalidade()) + fatura.getResiduoDescontado() + fatura.getValorServicosAdicionais());
	}

	/**
	 * Adiciona servicos
	 * 
	 * @param lista
	 */
	public void servicosAdicionais(Beneficiario beneficiario) {
		Double total = 0.00;
		for (Plano plano : planoRepository.findByServico(beneficiario)) {
			total += plano.getConvenio().getValor();
		}
		// TODO seta total dos valores adicionais
		fatura.setValorServicosAdicionais(total);
	}

	/**
	 * Tras o residuo da fatura anterior
	 *
	 * @param plano
	 * @return
	 */
	public Double aplicaResiduoNaFatura(Plano plano) {
		Double residuo = 0.00;
		for (Fatura f : repository.findByFaturaMesAno(mesAtual(), anoAtual())) {
			if (f.getPlano() == plano && f.getValorDoResiduo() != null) {

				residuo = f.getValorDoResiduo();
				fatura.setResiduoDescontado(f.getValorDoResiduo());
			} else {
				fatura.setResiduoDescontado(0.01);
			}
		}
		return residuo;
	}

	/**
	 * Tras o valor do plano do dependente
	 *
	 * @param beneficiario
	 * @return
	 */
	public Double valorDependente(Beneficiario beneficiario) {
		if (beneficiario.getDependentes().isEmpty()) {
			return 0.00;
		} else {
			Double valorDependente = 0.00;
			if (beneficiario.isApartamento()) {
				valorDependente = dependenteRepository.sumAcomodacaoApartamento(beneficiario);
			} else {
				valorDependente = dependenteRepository.sumAcomodacaoEnfermaria(beneficiario);
			}
			return valorDependente;
		}
	}

	public Double valorAcomodacao(Double valor) {
		return valor;
	}

	/**
	 * Gera itens da fatura
	 */
	public void geraItensDaFatura(Beneficiario beneficiario) {
		System.out.println(">>>>> geraItensDaFatura()");
		List<Plano> listaPlanos = planoRepository.findByPlanoAtivo(beneficiario);
		List<Dependente> listaDependentes = beneficiario.getDependentes();

		listaPlanos.forEach(plano -> {
			item.setIdentificacao(plano.getConvenio().getNome());
			item.setFatura(fatura);
			item.setOrdem(plano.getConvenio().getId());

			if (plano.getConvenio().getId() == 1) {
				item.setValor(beneficiario.getValorAcomodacao());

				// Se o convenio for mensalidade
			} else if (plano.getConvenio().getId() == 2) {

				Double mensalidade = 00.00;
				// Gera mensalidade para servidor
				if (plano.getBeneficiario().isBeneficiarioServidor()) {

					if (plano.getBeneficiario().isConsignado()) {
						item.setValor(mensalidade);
						item.setIdentificacao(plano.getConvenio().getNome() + " (CONSIGNADO)");

						// Calcula mensalidade para não consignado
					} else {
						// Mensalidade é 0,8% do salário
						mensalidade = plano.getBeneficiario().getSalario() * 0.008;
						fatura.setValorMensalidade(mensalidade);
					}
				} else {
					mensalidade = 70.00;
					item.setValor(mensalidade);
				}
			} else {
				item.setValor(plano.getConvenio().getValor());
			}

			itemDaFaturaRepository.save(item);
			item = new ItemDaFatura();

		});

		// Gerar item da fatura para os dependentes
		listaDependentes.forEach(dependente -> {
			item.setIdentificacao(dependente.getNome());
			item.setFatura(fatura);
			item.setValor(dependente.getValorAcomodacao());
			item.setOrdem(1l);

			itemDaFaturaRepository.save(item);
			item = new ItemDaFatura();
		});
	}

	/**
	 * Usado para fatura individual - apenas seta multa aplicada para true
	 */
	public void aplicaMultaPorAtraso(Fatura fatura) {
		Calendar hoje = Calendar.getInstance();
		int dia = hoje.get(Calendar.DAY_OF_MONTH);
		int mes = hoje.get(Calendar.MONTH);

		GregorianCalendar dataCal = new GregorianCalendar();
		dataCal.setTime(fatura.getVencimento());
		int diaDaFatura = dataCal.get(Calendar.DAY_OF_MONTH);
		int mesDaFatura = dataCal.get(Calendar.MONTH);

		if (dia > diaDaFatura && mes >= mesDaFatura) {
			System.out
					.println(">>>>> Aplicando multa por atraso para: " + fatura.getPlano().getBeneficiario().getNome());
			fatura.setMultaAplicada(true);

			repository.save(fatura);
		}
	}

	/**
	 * Calculo dos juros ao dia para fatura individual
	 */
	public void aplicaJurosAoDia(Fatura fatura) {
		Double juros = 0.00;
		Double multa = 0.00;
		Calendar hoje = Calendar.getInstance();
		multa = fatura.getValorTotalGerado() * 0.02;
		int ultimoDiaGerado = fatura.getDataJuros().get(Calendar.DAY_OF_MONTH);
		int dia = hoje.get(Calendar.DAY_OF_MONTH);
		int mes = hoje.get(Calendar.MONTH);

		GregorianCalendar dataCal = new GregorianCalendar();
		dataCal.setTime(fatura.getVencimento());
		int diaDaFatura = dataCal.get(Calendar.DAY_OF_MONTH);
		int mesDaFatura = dataCal.get(Calendar.MONTH);

		if (dia != ultimoDiaGerado && dia > diaDaFatura && mes >= mesDaFatura) {
			System.out.println(">>>>> Calculando juros para: " + fatura.getPlano().getBeneficiario().getNome());
			juros = fatura.getValorTotalGerado() * 0.00033 * fatura.getDiasAtrasados();

			System.out.println(">>>>> Juros: " + juros);
			System.out.println(">>>>> Multa: " + multa);
			System.out.println(">>>>> Residuo: " + fatura.getResiduoDescontado());

			Double calculo = juros + fatura.getValorTotalGerado() + multa + fatura.getResiduoDescontado();

			System.out.println(">>>>> Calculo: " + calculo);

			fatura.setValorTotal(calculo);
			fatura.setDataJuros(hoje);
			repository.save(fatura);
		}
	}

	/**
	 * Define o dia do vencimento para fatura individual
	 */
	public void dataDeVencimentoFaturaIndividual() {
		Date dataHoje = new java.util.Date();

		Calendar c = Calendar.getInstance();
		c.setTime(dataHoje);

		c.set(anoAtual(), mesAtual(), 5);

		Date vencimento = c.getTime();

		fatura.setVencimento(vencimento);
	}

	/**
	 * Informa o dia do mes
	 *
	 * @return
	 */
	public int diaDoMes() {
		Calendar hoje = Calendar.getInstance();
		hoje.get(Calendar.DAY_OF_MONTH);
		int diaMes = hoje.get(Calendar.DAY_OF_MONTH);

		return diaMes;
	}

	public int mesAtual() {
		Calendar hoje = Calendar.getInstance();
		hoje.get(Calendar.MONTH);
		int mes = hoje.get(Calendar.MONTH);

		return mes;
	}

	public int anoAtual() {
		Calendar hoje = Calendar.getInstance();
		hoje.get(Calendar.YEAR);
		int ano = hoje.get(Calendar.YEAR);

		return ano;
	}

	/**
	 * Verifica se ha fatura do mes
	 *
	 * @return
	 */
	public boolean isFaturaParaEsseMes() {
		return mesFaturaRepository.findByFaturaDoMes(mesAtual() + 1, anoAtual()).isEmpty();
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

}
