package br.com.aee.controller;

import java.io.Serializable;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import br.com.aee.model.Fatura;
import br.com.aee.model.ItemDaFatura;
import br.com.aee.repository.FaturaRepository;
import br.com.aee.repository.ItemDaFaturaRepository;
import br.com.aee.util.JsfUtil;

@Named("itemFaturaMB")
@ViewScoped
public class CadastroItemFaturaController implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private ItemDaFaturaRepository repository;
	
	@Inject
	private FaturaRepository faturaRepository;

	private ItemDaFatura itemDaFatura;
	
	private List<Fatura> listaFatura;

	// Actions

	@PostConstruct
	public void init() {
		itemDaFatura = new ItemDaFatura();
		listaFatura = faturaRepository.findAll();
	}

	/**
	 * Salva itemFatura
	 */
	public void save() {
		try {
			itemDaFatura.setOrdem(1L);
			repository.save(itemDaFatura);
			itemDaFatura = new ItemDaFatura();

			JsfUtil.info("Salvo com sucesso!");
			System.out.println("Salvo com sucesso");

		} catch (Exception e) {
			e.getMessage();
		}
	}

	public ItemDaFatura getItemDaFatura() {
		return itemDaFatura;
	}

	public void setItemDaFatura(ItemDaFatura itemDaFatura) {
		this.itemDaFatura = itemDaFatura;
	}
	
	public List<Fatura> getListaFatura() {
		return listaFatura;
	}

}
