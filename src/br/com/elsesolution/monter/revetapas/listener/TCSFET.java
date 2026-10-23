package br.com.elsesolution.monter.revetapas.listener;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collection;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;

public class TCSFET implements EventoProgramavelJava {

	public static final String PROP_LIBERA_ALTERACAO = "REVETAPAS_LIBERA_ALTERACAO";

	@Override
	public void afterUpdate(PersistenceEvent event) throws Exception {
	}

	@Override
	public void afterDelete(PersistenceEvent event) throws Exception {
	}

	@Override
	public void beforeCommit(TransactionContext ctx) throws Exception {
	}

	@Override
	public void beforeDelete(PersistenceEvent event) throws Exception {
	}

	@Override
	public void beforeInsert(PersistenceEvent event) throws Exception {
	}
	

	@Override
	public void afterInsert(PersistenceEvent event) throws Exception {
		DynamicVO linha = (DynamicVO) event.getVo();

		String motivo = linha.asString("AD_MOTIVOALTERACAO");
		if (motivo == null || motivo.trim().isEmpty()) {
			motivo = "Criação da etapa";
		}

		inserirNovaRevisao(linha, motivo);
	}

	@Override
	public void beforeUpdate(PersistenceEvent event) throws Exception {
		DynamicVO linha = (DynamicVO) event.getVo();
		DynamicVO linhaOld = (DynamicVO) event.getOldVO();

		if (houveAlteracaoDeData(linha, linhaOld)) {

			if (!Boolean.TRUE.equals(JapeSession.getProperty(PROP_LIBERA_ALTERACAO))) {
				throw new MGEModelException(
						"As datas previstas só podem ser alteradas pelo botão de ação \"[Alterar datas do Cronograma]\".");
			}

			String motivo = linha.asString("AD_MOTIVOALTERACAO");
			if (motivo == null || motivo.trim().isEmpty()) {
				throw new MGEModelException("Informe o motivo da alteração.");
			}

			inserirNovaRevisao(linha, motivo.trim());
		}
	}


	private boolean houveAlteracaoDeData(DynamicVO novo, DynamicVO antigo) {
		return dataDiferente(novo.asTimestamp("DTCEDOINIPREV"), antigo.asTimestamp("DTCEDOINIPREV"))
				|| dataDiferente(novo.asTimestamp("DTCEDOFIMPREV"), antigo.asTimestamp("DTCEDOFIMPREV"))
				|| dataDiferente(novo.asTimestamp("DTTARDEINIPREV"), antigo.asTimestamp("DTTARDEINIPREV"))
				|| dataDiferente(novo.asTimestamp("DTTARDEFIMPREV"), antigo.asTimestamp("DTTARDEFIMPREV"));
	}

	private boolean dataDiferente(Timestamp dataNova, Timestamp dataAntiga) {
		if (dataNova == null && dataAntiga == null)
			return false;
		if (dataNova == null || dataAntiga == null)
			return true;
		return !dataNova.equals(dataAntiga);
	}

	private void inserirNovaRevisao(DynamicVO linha, String motivo) throws Exception {

		JapeWrapper revDAO = JapeFactory.dao("AD_REVETAPAS");

		BigDecimal nufap = linha.asBigDecimal("NUFAP");
		BigDecimal numEtapa = linha.asBigDecimal("NUMETAPA");

		Collection<DynamicVO> revVOs = revDAO.find("this.NUFAP = ? and this.NUMETAPA = ?", nufap, numEtapa);

		BigDecimal proximaRev = BigDecimal.ZERO;

		if (revVOs != null && !revVOs.isEmpty()) {
			BigDecimal maxRev = BigDecimal.ZERO;

			for (DynamicVO vo : revVOs) {
				BigDecimal revAtual = vo.asBigDecimal("NUREV");
				if (revAtual != null && revAtual.compareTo(maxRev) > 0) {
					maxRev = revAtual;
				}
			}
			proximaRev = maxRev.add(BigDecimal.ONE);
		}

		BigDecimal codusu = AuthenticationInfo.getCurrent().getUserID(); 
		
		revDAO.create().set("NUREV", proximaRev).set("NUMETAPA", numEtapa).set("NUFAP", nufap)
				.set("DTCEDOINIPREV", linha.asTimestamp("DTCEDOINIPREV"))
				.set("DTCEDOFIMPREV", linha.asTimestamp("DTCEDOFIMPREV"))
				.set("DTTARDEINIPREV", linha.asTimestamp("DTTARDEINIPREV"))
				.set("DTTARDEFIMPREV", linha.asTimestamp("DTTARDEFIMPREV")).set("MOTIVOALTERACAO", motivo)
				.set("CODUSUALT", codusu).save();
	}
}