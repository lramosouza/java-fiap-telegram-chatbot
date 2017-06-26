package com.telegrambotbank.main;

import java.math.BigDecimal;
import java.util.List;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.TelegramBotAdapter;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ChatAction;
import com.pengrad.telegrambot.request.GetUpdates;
import com.pengrad.telegrambot.request.SendChatAction;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.BaseResponse;
import com.pengrad.telegrambot.response.GetUpdatesResponse;
import com.pengrad.telegrambot.response.SendResponse;
import com.telegrambotbank.datatype.ClienteVO;
import com.telegrambotbank.datatype.ContaBancariaVO;
import com.telegrambotbank.datatype.DependenteVO;
import com.telegrambotbank.datatype.EmprestimoVO;
import com.telegrambotbank.datatype.LancamentoVO;
import com.telegrambotbank.enumeration.OpcoesBotEnum;
import com.telegrambotbank.enumeration.TipoContaCorrenteEnum;
import com.telegrambotbank.messages.GeneralMessages;
import com.telegrambotbank.opcoes.helper.DependenteHelper;
import com.telegrambotbank.opcoes.helper.DepositoBancarioHelper;
import com.telegrambotbank.opcoes.helper.EmprestimoHelper;
import com.telegrambotbank.opcoes.mediator.OpcoesMediator;
import com.telegrambotbank.opcoes.util.ClienteUtil;
import com.telegrambotbank.opcoes.util.Utils;

public class Main {
	public static void main(String[] args) {
		// Mediator respons�vel por verificar qual � a a��o a ser tomada de
		// acordo com a op��o desejada
		OpcoesMediator opcoesMediator = new OpcoesMediator();

		// Cria��o do objeto bot com as informa��es de acesso
		TelegramBot bot = TelegramBotAdapter.build("332862407:AAGAwq3hj0XGS3y_TlrWtkQuc2Lh8deSes0");

		// objeto respons�vel por receber as mensagens
		GetUpdatesResponse updatesResponse;

//		objeto respons�vel por gerenciar o envio de respostas
		SendResponse sendResponse;
		
		// objeto respons�vel por gerenciar o envio de a��es do chat
		BaseResponse baseResponse;
		
		// Mockery TODO retirar - IN�CIO

		ContaBancariaVO contaCorrenteDepositante = new ContaBancariaVO();
		contaCorrenteDepositante.setAgenciaBancaria("6252");
		contaCorrenteDepositante.setNuContaCorrete("176117");
		contaCorrenteDepositante.setTipo(TipoContaCorrenteEnum.SIMPLES);

		/*ClienteVO cliente = new ClienteVO();
		cliente.setCPF(new BigDecimal("42847256881"));
		cliente.setDataNascimento(new Date());
		cliente.setEmail("teste@teste.com.br");
		cliente.setNome("Teste Leandro");*/

		//contaCorrenteDepositante.setCliente(cliente);

		// Mockery TODO retirar - FIM
		// controle de off-set, isto �, a partir deste ID ser� lido as
		// mensagens
		// pendentes na fila
		int m = 0;

		// loop infinito pode ser alterado por algum timer de intervalo curto
		while (true) {
			// executa comando no Telegram para obter as mensagens pendentes a
			// partir de um off-set (limite inicial)
			updatesResponse = bot.execute(new GetUpdates().limit(100).offset(m));

			// lista de mensagens
			List<Update> updates = updatesResponse.updates();

			// an�lise de cada a��o da mensagem
			for (Update update : updates) {
			
				// atualiza��o do off-set
				m = update.updateId() + 1;

				String mensagemRecebida = update.message().text();

				System.out.println("Recebendo mensagem:" + mensagemRecebida);


			
					if(OpcoesBotEnum.START.getOpcaoDesejada().equalsIgnoreCase(mensagemRecebida)){
						baseResponse = bot.execute(new SendChatAction(update.message().chat().id(), ChatAction.typing.name()));
						sendResponse = bot.execute(new SendMessage(update.message().chat().id(), new GeneralMessages().getMsgBoasVindas() ));
						mensagemRecebida = "";
					}
					
					if(OpcoesBotEnum.EMPRESTIMO.getOpcaoDesejada().equalsIgnoreCase(mensagemRecebida)){
						EmprestimoVO emprestimoVO = new EmprestimoVO();
						try{
							//TODO Mock teste emprestimo
							BigDecimal saldo = new BigDecimal(10000);
							
							emprestimoVO.setVlContratado(EmprestimoHelper.valorEmprestimoDisponivel(bot, update, saldo));
							emprestimoVO.setPrazo(EmprestimoHelper.prazoEmprestimo(bot, update));
							emprestimoVO.setVlCalculado(EmprestimoHelper.calculaEmprestimo(bot, update, emprestimoVO));
						} catch(Exception e){
							sendResponse = bot.execute(new SendMessage(update.message().chat().id(), e.getMessage()));
						}
					}
					
					if (OpcoesBotEnum.DEPOSITAR.getOpcaoDesejada().equalsIgnoreCase(mensagemRecebida)) {
						ContaBancariaVO contaCorrenteDestino = new ContaBancariaVO();
				
						String mensagemRetorno = null ;
						
						try{
							
							BigDecimal valorDeposito = DepositoBancarioHelper.solicitarValorInformadoDeposito(bot, update);

							contaCorrenteDestino.setAgenciaBancaria(DepositoBancarioHelper.solicitarNuAgenciaDestinoDeposito(bot, update));

							contaCorrenteDestino.setNuContaCorrete(DepositoBancarioHelper.solicitarNuContaDestinoDeposito(bot, update, valorDeposito));
							
							mensagemRetorno = opcoesMediator.depositar(contaCorrenteDepositante, contaCorrenteDestino, valorDeposito);
							
							baseResponse = bot.execute(new SendChatAction(update.message().chat().id(), ChatAction.typing.name()));
							sendResponse = bot.execute(new SendMessage(update.message().chat().id(), mensagemRetorno));
							mensagemRecebida = "";
							
						}catch(Exception e){
							sendResponse = bot.execute(new SendMessage(update.message().chat().id(), e.getMessage()));
						}						
					}
					
					if (OpcoesBotEnum.INCLUIR_DEPENDENTE.getOpcaoDesejada().equalsIgnoreCase(mensagemRecebida)) {
						DependenteVO dependente = new DependenteVO();

						String mensagemRetorno = null;
						
						try{
							dependente.setNomeDependente(DependenteHelper.solicitarNomeDependente(bot, update));
						
							dependente.setCpfDependente(DependenteHelper.solicitarCPFDependente(bot, update, dependente));
							
//							TODO TIRAR MOCK DA CONTA						
							LancamentoVO dadosOperacao = new LancamentoVO();
							dadosOperacao.setContaBancaria(contaCorrenteDepositante.getNuContaCorrete().trim());
							dadosOperacao.setAgenciaBancaria(contaCorrenteDepositante.getAgenciaBancaria().trim());
//							TODO TIRAR MOCK DA CONTA
							
							mensagemRetorno = opcoesMediator.cadastrarDependente(dependente, dadosOperacao);
							
							baseResponse = bot.execute(new SendChatAction(update.message().chat().id(), ChatAction.typing.name()));
							sendResponse = bot.execute(new SendMessage(update.message().chat().id(), mensagemRetorno));
							mensagemRecebida = "";
							
						}catch(Exception e){
							sendResponse = bot.execute(new SendMessage(update.message().chat().id(), e.getMessage()));
						}						
					}
					if (OpcoesBotEnum.EXIBIR_MINHAS_INFORMACOES.getOpcaoDesejada().equalsIgnoreCase(mensagemRecebida)){
						String mensagemRetorno = null;
						
//						TODO TIRAR MOCK DA CONTA						
						LancamentoVO dadosOperacao = new LancamentoVO();
						dadosOperacao.setContaBancaria(contaCorrenteDepositante.getNuContaCorrete().trim());
						dadosOperacao.setAgenciaBancaria(contaCorrenteDepositante.getAgenciaBancaria().trim());
//						TODO TIRAR MOCK DA CONTA
						
						try{
							mensagemRetorno = opcoesMediator.exibirInformacoesConta(dadosOperacao);
							baseResponse = bot.execute(new SendChatAction(update.message().chat().id(), ChatAction.typing.name()));
							sendResponse = bot.execute(new SendMessage(update.message().chat().id(), mensagemRetorno));
							mensagemRecebida = "";
						}catch (Exception e){
							sendResponse = bot.execute(new SendMessage(update.message().chat().id(), e.getMessage()));
						}
						
					}
				    if(OpcoesBotEnum.CRIAR_CONTA.getOpcaoDesejada().equalsIgnoreCase(mensagemRecebida)) {				    	
				    	
				    	ClienteVO cli = new ClienteVO();
				    	
				    	cli.setNome(ClienteUtil.solicitarNomeCliente(bot, update));
				    	cli.setCPF(ClienteUtil.solicitarCpfCliente(bot, update));
				    	cli.setDataNascimento(ClienteUtil.solicitarDtNascCliente(bot, update));
				    	cli.setEmail(ClienteUtil.solicitarEmailCliente(bot, update));

				    	
				    	baseResponse = bot.execute(new SendChatAction(update.message().chat().id(), ChatAction.typing.name()));
						sendResponse = bot.execute(new SendMessage(update.message().chat().id(), "Estamos criando sua conta..."));
				    	
				    	ContaBancariaVO cntBancaria = new ContaBancariaVO();
				    	cntBancaria.setAgenciaBancaria(Utils.agencia());
				    	cntBancaria.setNuContaCorrete(Utils.gerarContaCorrente());
				    	cntBancaria.setCliente(cli);
						
				    	String mensagemRetorno = "Pronto! Anote o n�mero da sua conta: \n"
				    			+ "Ag�ncia: " + cntBancaria.getAgenciaBancaria() + "\n"
				    			+ "Conta: " + cntBancaria.getNuContaCorrete();
				
				    	baseResponse = bot.execute(new SendChatAction(update.message().chat().id(), ChatAction.typing.name()));
						sendResponse = bot.execute(new SendMessage(update.message().chat().id(), mensagemRetorno));
						mensagemRecebida = "";
						updatesResponse = bot.execute(new GetUpdates().limit(100).offset(m+2));
						
					}
				    if(OpcoesBotEnum.HELP.getOpcaoDesejada().equalsIgnoreCase(mensagemRecebida)){
				    	baseResponse = bot.execute(new SendChatAction(update.message().chat().id(), ChatAction.typing.name()));
						sendResponse = bot.execute(new SendMessage(update.message().chat().id(), new GeneralMessages().getMsgHelp()));
						mensagemRecebida = "";
						updatesResponse = bot.execute(new GetUpdates().limit(100).offset(m+2));
				    }
				    if(OpcoesBotEnum.TARIFAS.getOpcaoDesejada().equalsIgnoreCase(mensagemRecebida)){
				    	baseResponse = bot.execute(new SendChatAction(update.message().chat().id(), ChatAction.typing.name()));
						sendResponse = bot.execute(new SendMessage(update.message().chat().id(), new GeneralMessages().getTarifas()));
						mensagemRecebida = "";
						updatesResponse = bot.execute(new GetUpdates().limit(100).offset(m+2));
				    }

//					baseResponse = bot.execute(new SendChatAction(update.message().chat().id(), ChatAction.typing.name()));
//					sendResponse = bot.execute(new SendMessage(update.message().chat().id(), e.getMessage()));
//				mensagemRecebida = "";
//				
				
				
				// envio de "Escrevendo" antes de enviar a resposta
//				baseResponse = bot.execute(new SendChatAction(update.message().chat().id(), ChatAction.typing.name()));

				// verifica��o de a��o de chat foi enviada com sucesso
//				System.out.println("Resposta de Chat Action Enviada?" + baseResponse.isOk());

				// envio da mensagem de resposta
				//sendResponse = bot.execute(new
				 //SendMessage(update.message().chat().id(), "N�o entend..."));

				// verifica��o de mensagem enviada com sucesso
//				System.out.println("Mensagem Enviada?" +
//						baseResponse.isOk());
				

			}
		}
	}

}