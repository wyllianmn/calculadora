const elExpressao = document.getElementById("expressao");
const elResultado = document.getElementById("resultado");
const elHistorico = document.getElementById("historico");
const elLista = document.getElementById("lista-historico");

let expressao = "";
let acabouDeCalcular = false;

function mostrar(textoResultado, ehErro = false) {
  elExpressao.textContent = expressao.replace(/\*/g, "×").replace(/\//g, "÷") || " ";
  elResultado.textContent = textoResultado;
  elResultado.classList.toggle("erro", ehErro);
}

function adicionar(valor) {
  const ehOperador = "+-*/".includes(valor);
  // Depois de um "=", continuar com operador usa o resultado; número começa do zero.
  if (acabouDeCalcular && !ehOperador) expressao = "";
  acabouDeCalcular = false;
  if (expressao.length >= 100) return;
  expressao += valor;
  mostrar(expressao ? "" : "0");
}

function apagar() {
  acabouDeCalcular = false;
  expressao = expressao.slice(0, -1);
  mostrar(expressao ? "" : "0");
}

function limpar() {
  acabouDeCalcular = false;
  expressao = "";
  mostrar("0");
}

function registrarHistorico(conta, resultado) {
  elHistorico.hidden = false;
  const item = document.createElement("li");
  const esquerda = document.createElement("span");
  esquerda.textContent = conta.replace(/\*/g, "×").replace(/\//g, "÷");
  const direita = document.createElement("strong");
  direita.textContent = "= " + resultado;
  item.append(esquerda, direita);
  elLista.prepend(item);
  while (elLista.children.length > 5) elLista.lastChild.remove();
}

async function calcular() {
  if (!expressao) return;
  try {
    // O cálculo é feito no backend Java.
    const resposta = await fetch("/api/calcular?expr=" + encodeURIComponent(expressao));
    const dados = await resposta.json();
    if (!resposta.ok) {
      mostrar(dados.erro || "Erro", true);
      return;
    }
    registrarHistorico(expressao, dados.resultado);
    mostrar(dados.resultado);
    expressao = dados.resultado;
    acabouDeCalcular = true;
  } catch (e) {
    mostrar("Sem conexão com o servidor", true);
  }
}

document.querySelector(".teclas").addEventListener("click", (evento) => {
  const botao = evento.target.closest("button");
  if (!botao) return;
  if (botao.dataset.valor) adicionar(botao.dataset.valor);
  else if (botao.dataset.acao === "limpar") limpar();
  else if (botao.dataset.acao === "apagar") apagar();
  else if (botao.dataset.acao === "calcular") calcular();
});

// Teclado
document.addEventListener("keydown", (evento) => {
  const tecla = evento.key;
  if (evento.ctrlKey || evento.metaKey || evento.altKey) return;

  if (/^[0-9+\-*/().]$/.test(tecla)) {
    adicionar(tecla);
  } else if (tecla === ",") {
    adicionar(".");
  } else if (tecla === "Enter" || tecla === "=") {
    evento.preventDefault();
    calcular();
  } else if (tecla === "Backspace") {
    apagar();
  } else if (tecla === "Escape" || tecla.toLowerCase() === "c") {
    limpar();
  }
});
