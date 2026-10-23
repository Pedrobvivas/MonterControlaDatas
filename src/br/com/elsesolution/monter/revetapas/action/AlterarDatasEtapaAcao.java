package br.com.elsesolution.monter.revetapas.action;

import java.math.BigDecimal;
import java.sql.Timestamp;

import br.com.elsesolution.monter.revetapas.listener.TCSFET;
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidUpdateVO;


public class AlterarDatasEtapaAcao implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao ctx) throws Exception {

		Registro[] linhas = ctx.getLinhas();
		if (linhas == null || linhas.length != 1) {
			ctx.mostraErro("Selecione uma única FAP.");
			return;
		}

		BigDecimal nufap = (BigDecimal) linhas[0].getCampo("NUFAP");

		Object paramEtapa = ctx.getParam("P_NUMETAPA");
		if (paramEtapa == null) {
			ctx.mostraErro("Informe o número da etapa.");
			return;
		}
		BigDecimal numEtapa = new BigDecimal(paramEtapa.toString());

		String motivo = (String) ctx.getParam("P_MOTIVO");
		if (motivo == null || motivo.trim().isEmpty()) {
			ctx.mostraErro("Informe o motivo da alteração.");
			return;
		}

		Timestamp cedoIni = (Timestamp) ctx.getParam("P_DTCEDOINIPREV");
		Timestamp cedoFim = (Timestamp) ctx.getParam("P_DTCEDOFIMPREV");
		Timestamp tardeIni = (Timestamp) ctx.getParam("P_DTTARDEINIPREV");
		Timestamp tardeFim = (Timestamp) ctx.getParam("P_DTTARDEFIMPREV");

		if (cedoIni == null && cedoFim == null && tardeIni == null && tardeFim == null) {
			ctx.mostraErro("Informe ao menos uma data para alterar.");
			return;
		}

		JapeWrapper etapaDAO = JapeFactory.dao("EtapaProjeto");

		DynamicVO etapaVO = etapaDAO.findOne("this.NUFAP = ? and this.NUMETAPA = ?", nufap, numEtapa);
		if (etapaVO == null) {
			ctx.mostraErro("A etapa " + numEtapa + " não existe na FAP " + nufap + ".");
			return;
		}

		boolean houveMudanca = mudou(cedoIni, etapaVO.asTimestamp("DTCEDOINIPREV"))
				|| mudou(cedoFim, etapaVO.asTimestamp("DTCEDOFIMPREV"))
				|| mudou(tardeIni, etapaVO.asTimestamp("DTTARDEINIPREV"))
				|| mudou(tardeFim, etapaVO.asTimestamp("DTTARDEFIMPREV"));

		if (!houveMudanca) {
			ctx.setMensagemRetorno("As datas informadas são iguais às atuais. Nenhuma alteração foi feita.");
			return;
		}

		try {
			JapeSession.putProperty(TCSFET.PROP_LIBERA_ALTERACAO, Boolean.TRUE);

			FluidUpdateVO upd = etapaDAO.prepareToUpdate(etapaVO);

			if (cedoIni != null)
				upd.set("DTCEDOINIPREV", cedoIni);
			if (cedoFim != null)
				upd.set("DTCEDOFIMPREV", cedoFim);
			if (tardeIni != null)
				upd.set("DTTARDEINIPREV", tardeIni);
			if (tardeFim != null)
				upd.set("DTTARDEFIMPREV", tardeFim);

			upd.set("AD_MOTIVOALTERACAO", motivo.trim());
			upd.update();
		} finally {
			JapeSession.putProperty(TCSFET.PROP_LIBERA_ALTERACAO, Boolean.FALSE);
		}

		ctx.setMensagemRetorno("Etapa " + numEtapa + " da FAP " + nufap + " atualizada com sucesso.");
	}

	private boolean mudou(Timestamp informada, Timestamp atual) {
		return informada != null && !informada.equals(atual);
	}
}