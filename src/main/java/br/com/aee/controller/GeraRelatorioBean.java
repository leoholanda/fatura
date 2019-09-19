package br.com.aee.controller;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;

import br.com.aee.model.Beneficiario;
import br.com.aee.model.Dependente;
import br.com.aee.model.Fatura;
import br.com.aee.report.ExecutorRelatorio;
import br.com.aee.repository.BeneficiarioRepository;
import br.com.aee.repository.DependenteRepository;
import br.com.aee.repository.FaturaRepository;
import br.com.aee.util.JsfUtil;

@Named("geraRelatorioMB")
@ViewScoped
public class GeraRelatorioBean implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private FacesContext facesContext;

	@Inject
	private HttpServletResponse response;

	@Inject
	private EntityManager manager;

	@Inject
	private FaturaRepository faturaRepository;

	@Inject
	private DependenteRepository dependenteRepository;
	
	@Inject 
	private BeneficiarioRepository beneficiarioRepository;

	private Long idBeneficiario;

	private Integer searchAno;

	private Double valor;
	
	private Beneficiario beneficiario;

	private List<Dependente> listaDeDependenteDoBeneficiario;

	private Double valorDaFaixaEtaria;

	/**
	 * Lista fatura anual por beneficiário
	 *
	 * @return
	 */
	public List<Fatura> getListaFaturaAnualDoBeneficiario() {
		List<Fatura> listEmpty = new ArrayList<>();
		if (idBeneficiario == null || searchAno == null) {
			return listEmpty;
		} else {
			return faturaRepository.findByFaturaBeneficiarioAno(idBeneficiario, searchAno);
		}
	}

	public boolean isPossuiDependente() {
		return !dependenteRepository.findByIdBeneficiario(idBeneficiario).isEmpty();
	}

	/**
	 * Emite declaracao imposto de renda pf
	 */
	public void emitirDeclaracao() {
		List<Fatura> faturas = this.getListaFaturaAnualDoBeneficiario();
		listaDeDependenteDoBeneficiario = dependenteRepository.findByIdBeneficiario(idBeneficiario);
		faturas = faturaRepository.findByFaturaBeneficiarioAno(idBeneficiario, searchAno);
		System.out.println(">>> " + idBeneficiario + " " + searchAno);

		if (faturas.isEmpty()) {
			JsfUtil.error("Nenhum registro encontrado");
		}
	}

	/**
	 * Exporta para pdf
	 */
	public void emitirDeclaracaoImpostoDeRendaPDF() {
		if (idBeneficiario != null) {
			//Localiza o beneficiario
			Beneficiario beneficiario = new Beneficiario();
			beneficiario = beneficiarioRepository.findBy(idBeneficiario);
			
			Map<String, Object> parametros = new HashMap<>();
			parametros.put("p_beneficiario_id", this.idBeneficiario);
			parametros.put("p_ano", this.searchAno);

			ExecutorRelatorio executor = new ExecutorRelatorio("/relatorios/declaracao-irpf-titular.jasper", this.response,
					parametros, "Declaração Imposto de Renda - " + beneficiario.getNomeComIniciaisMaiuscula() + ".pdf");

			Session session = manager.unwrap(Session.class);
			session.doWork(executor);

			if (executor.isRelatorioGerado()) {
				facesContext.responseComplete();
			} else {
				JsfUtil.error("A execução do relatório não retornou dados");
			}
			
			// Emite para dependente
//			System.out.println(">>> Chamando dependente");
//			this.emitirDeclaracaoImpostoDeRendaDependente();
		} else {
			JsfUtil.error("A execução do relatório não retornou dados");
		}
	}

	/**
	 * Exporta para pdf
	 */
	public void emitirDeclaracaoImpostoDeRendaDependente() {
		System.out.println(">>> Invocando método");
		listaDeDependenteDoBeneficiario = dependenteRepository.findByIdBeneficiario(idBeneficiario);
		if (!listaDeDependenteDoBeneficiario.isEmpty()) {
			System.out.println(">>> Entrou");
			for (Dependente dependente : listaDeDependenteDoBeneficiario) {
				Map<String, Object> parametros = new HashMap<>();
				parametros.put("p_beneficiario_id", this.idBeneficiario);
				parametros.put("p_ano", this.searchAno);
				parametros.put("p_dependente", dependente.getNome());
				
				System.out.println(">>> beneficiario: " + this.idBeneficiario);
				System.out.println(">>> ano: " + this.searchAno);
				System.out.println(">>> Dependente: " + dependente.getNome());

				ExecutorRelatorio executor2 = new ExecutorRelatorio("/relatorios/declaracao-irpf-dependente.jasper",
						this.response, parametros, "Declaração Imposto de Renda - " + dependente.getNomeComIniciaisMaiuscula() + ".pdf");

				Session session = manager.unwrap(Session.class);
				session.doWork(executor2);
				
				System.out.println(">>> Gerando Declaração");

				if (executor2.isRelatorioGerado()) {
					facesContext.responseComplete();
				} else {
					JsfUtil.error("A execução do relatório não retornou dados");
				}
			}
		}
	}

	/**
	 * Busca id do beneficiario
	 */
	public void buscar() {
		System.out.println(">>> " + idBeneficiario);
		beneficiario = beneficiarioRepository.findBy(idBeneficiario);
	}
	
	public Beneficiario getBeneficiario() {
		return beneficiario;
	}
	
	public void setBeneficiario(Beneficiario beneficiario) {
		this.beneficiario = beneficiario;
	}

	public Long getIdBeneficiario() {
		return idBeneficiario;
	}

	public void setIdBeneficiario(Long idBeneficiario) {
		this.idBeneficiario = idBeneficiario;
	}

	public Integer getSearchAno() {
		return searchAno;
	}

	public void setSearchAno(Integer searchAno) {
		this.searchAno = searchAno;
	}

	public Double getValor() {
		return valor;
	}

	public void setValor(Double valor) {
		this.valor = valor;
	}

	public List<Dependente> getListaDeDependenteDoBeneficiario() {
		return listaDeDependenteDoBeneficiario;
	}

	public List<Dependente> getDadosDoDependente(Dependente dependente) {
		return dependenteRepository.findById(dependente.getId());
	}

	public Double getValorDaFaixaEtaria() {
		return valorDaFaixaEtaria;
	}

	public void setValorDaFaixaEtaria(Double valorDaFaixaEtaria) {
		this.valorDaFaixaEtaria = valorDaFaixaEtaria;
	}
}
