# Desafios de PDI (2026-02)

Implementação dos cinco desafios do enunciado `ex.docx`. Para usar, abra uma imagem e
escolha o item no menu **Desafios**. O painel da direita mostra a imagem anotada e uma
janela abre com a tabela de resultados.

Sem imagem aberta, os itens do menu mostram um aviso. Qualquer erro durante a execução
aparece numa janela de erro, e o stack trace sai no console.

O código fica no pacote `dev.edinho.pdi.challenges`. Há dois conjuntos de imagens de exemplo:

| Pasta                          | Conteúdo                                                          | Testes                         |
|--------------------------------|-------------------------------------------------------------------|--------------------------------|
| `imagens/desafios/`            | imagens originais do enunciado (1536x1024), extraídas do zip      | `FullResolutionChallengesTest` |
| `src/test/resources/desafios/` | versões reduzidas embutidas no `ex.docx` (cerca de 400 a 1300 px) | `ChallengesTest`               |

Todas as medidas são em pixels da imagem original: área é a contagem de pixels e perímetro
é o comprimento do contorno (detalhes em [Base comum](#base-comum)).

## Escala de trabalho

Os parâmetros (raios de morfologia, limites de gradiente, margens) estão em pixels. Para
que funcionem igual em qualquer resolução, os desafios 1, 3, 4 e 5 reduzem a imagem até
800 px no maior lado antes de analisar (`WorkingImage`, redução por média de área); imagens
menores não mudam. Depois as medidas voltam à escala original: áreas são multiplicadas por
`escala²` e perímetros por `escala`. A imagem anotada é mostrada na escala de trabalho.

O desafio 2 trabalha na resolução original, porque reduzir apagaria as molduras finas, e o
reconhecimento já é independente de escala (a grade de densidade é normalizada pelo
retângulo do algarismo).

## 1. Controle de qualidade de porcas

Classe `NutInspector`. Menu **Desafios > 1. Porcas**.

Pipeline:

1. Tons de cinza e suavização gaussiana 5x5.
2. O nível do fundo é a mediana dos pixels da moldura da imagem.
3. Um pixel é da porca quando atende a pelo menos uma condição:
   - é bem mais escuro que o fundo (mais de 45 níveis abaixo);
   - é mais claro que o fundo (mais de 20 níveis acima), o que cobre os reflexos do metal;
   - está numa borda forte (gradiente de Sobel acima de 30).
4. Fechamento (raio 4), preenchimento de buracos e abertura (raio 3) dão a silhueta de cada
   porca. Cada componente conexo com pelo menos 0,2% da imagem é uma porca.
5. Furo: crescimento de região a partir do centroide da porca, avançando só por pixels de
   gradiente baixo. A parede interna do furo é uma rampa suave de sombra, e a borda do furo é
   um degrau forte, então o crescimento para na borda. O limite de gradiente é tentado do
   maior para o menor (40, 32, 25, 18, 12). Vale o primeiro que não vaza até a borda da
   porca e que ocupa entre 2% e 50% dela.

Resultados por porca, numeradas em ordem de leitura (linhas de cima para baixo, da esquerda
para a direita):

| Campo         | Significado                                          |
|---------------|------------------------------------------------------|
| Área          | pixels de metal (silhueta menos o furo)              |
| Per. ext      | perímetro do contorno externo da silhueta            |
| Per. int      | perímetro do furo                                    |
| Furo          | área do furo                                         |

Na imagem anotada, o contorno externo aparece em vermelho, o furo em azul com borda verde e
o número em amarelo. Uma porca sem furo detectado aparece como "sem furo" no relatório.

Limitação: a sombra projetada na base da porca pode entrar alguns pixels na silhueta,
aumentando um pouco a área e o perímetro externo.

## 2. Número par ou ímpar

Classe `DigitRecognizer`. Menu **Desafios > 2. Número par ou ímpar**.

Pré-processamento:

1. Tons de cinza e limiar de Otsu. A tinta é a classe minoritária da imagem; por isso uma
   moldura preta encostada na borda não inverte o resultado.
2. Componentes conexos (vizinhança 8). São descartados:
   - componentes que tocam a borda da imagem;
   - molduras: componentes que envolvem outro componente dentro de um furo;
   - componentes com menos da metade da altura do maior, como legendas.

Reconhecimento por comparação com modelos (vizinho mais próximo):

- Os modelos são os algarismos 0 a 9 desenhados em tempo de execução com as fontes lógicas
  do Java (SansSerif, Dialog, Serif e Monospaced, em negrito e normal), 80 modelos no total.
- Características de cada algarismo:
  - densidade de tinta numa grade 6x9 sobre o retângulo envolvente;
  - razão largura/altura;
  - número de furos (o 8 tem 2; o 0, 4, 6 e 9 têm 1; os demais, 0);
  - posição vertical média dos furos (separa o 6, com furo embaixo, do 9, com furo em cima).
- Só são comparados os modelos com o mesmo número de furos. Se nenhum tiver, compara com
  todos.

Saída: o número reconhecido e a classificação (par ou ímpar). Funciona com uma imagem de um
único algarismo, como pede o enunciado. Se houver vários, cada um é reconhecido e listado em
ordem de leitura. Na imagem anotada, pares aparecem em azul e ímpares em laranja.

## 3. Controle de qualidade de comprimidos

Classe `PillInspector`. Menu **Desafios > 3. Comprimidos**.

Segmentação: suavização gaussiana, limiar de Otsu (o fundo é a classe predominante na
moldura), fechamento (raio 3), abertura (raio 2) e preenchimento de buracos. Cada componente
com pelo menos 0,2% da imagem, que não toque a borda, é um comprimido.

Medidas por comprimido:

| Campo     | Definição                                                                          |
|-----------|------------------------------------------------------------------------------------|
| Área      | pixels da silhueta                                                                 |
| Perímetro | comprimento do contorno                                                            |
| Circul.   | circularidade `4πA / P²` (1 = círculo perfeito)                                    |
| Solidez   | área / área do casco convexo; cai quando falta um pedaço                           |
| Razão     | lado maior / lado menor do retângulo de área mínima; cresce em comprimidos ovais   |
| Fissura   | comprimento da maior estrutura escura fina / diâmetro equivalente                  |
| Manchas   | quantidade de pequenas falhas escuras na superfície                                |

Fissuras e manchas vêm de um black-hat 3x3 (fechamento menos imagem) sobre os pixels internos
do comprimido, a pelo menos 6% do diâmetro da borda. O black-hat realça estruturas escuras de
até 2 px de largura, como rachaduras, e ignora o sulco central, que é mais largo. Pixels com
contraste de pelo menos 30 são agrupados por proximidade (2 px).

Classificação (um comprimido pode ter mais de um problema):

| Problema              | Regra                                                    |
|-----------------------|----------------------------------------------------------|
| pequeno / grande      | área fora de ±15% da mediana dos comprimidos da imagem   |
| quebrado              | solidez mais de 0,007 abaixo da mediana dos comprimidos  |
| deformado             | razão > 1,10                                             |
| irregular             | circularidade < 0,80 (quando não é deformado)            |
| rachado               | fissura >= 0,15 do diâmetro                              |
| superfície danificada | 5 ou mais manchas (quando não é rachado)                 |

Na imagem anotada, cada comprimido aparece numerado, em verde quando aprovado e em vermelho
quando tem problema. A tabela traz todas as medidas e a situação.

Limitação: furinhos isolados na superfície (uma ou duas falhas) não são marcados, para
evitar falsos positivos com a textura do comprimido.

## 4. Contagem de círculos e quadrados

Classe `ShapeCounter`. Menu **Desafios > 4. Círculos e quadrados**.

1. Pixels escuros (todos os canais abaixo de 90) são linhas de contorno e separam as regiões.
2. As demais regiões são formadas por crescimento de região por cor. A semente é sempre um
   pixel "plano" (vizinhança 3x3 com diferença de até 12 por canal), para não começar numa
   borda com antialiasing. Entram na região os vizinhos-4 cuja diferença máxima por canal
   RGB em relação à cor da semente é de até 60. Comparar com a semente, e não com o vizinho,
   impede que a região atravesse degradês (por exemplo, do branco ao vermelho).
3. São descartadas regiões muito pequenas (menos de 0,1% da imagem) e as que tocam a borda
   (o fundo).
4. Cada região tem os furos preenchidos. Assim, o quadrado vermelho com um círculo branco
   dentro conta como quadrado, e o círculo conta separadamente.
5. Classificação pelo retângulo de área mínima (rotating calipers sobre o casco convexo),
   que também reconhece quadrados girados, como o losango azul do tabuleiro:

| Forma    | Razão dos lados | Preenchimento (área / área do retângulo)                 |
|----------|-----------------|----------------------------------------------------------|
| Quadrado | <= 1,15         | >= 0,88                                                  |
| Círculo  | <= 1,15         | entre 0,70 e 0,86 (π/4 ≈ 0,785) e circularidade >= 0,80  |

Outras formas (triângulos, faixas, retângulos alongados) são ignoradas. A saída traz o total
de círculos e quadrados, e a área e o perímetro de cada um. Na imagem, círculos aparecem em
magenta com rótulo `C` e quadrados em ciano com rótulo `Q`.

Limitação: a área medida é a do interior colorido, sem a linha preta de contorno.

## 5. Ferrugem em folhas

Classe `LeafRustAnalyzer`. Menu **Desafios > 5. Ferrugem em folhas**.

1. Conversão para HSV.
2. Folha: pixels com saturação acima de 0,20. O fundo branco ou cinza e a sombra têm
   saturação baixa. Depois vêm fechamento e abertura (raio 2), o maior componente conexo e o
   preenchimento de buracos (reflexos sem cor dentro da folha).
3. Ferrugem: pixels da folha com matiz entre 0° e 55°, ou acima de 340°, e saturação de pelo
   menos 0,25 (amarelo, laranja e marrom). O verde saudável fica acima de 60°.
4. O percentual é `área com ferrugem / área da folha`.

| Classificação     | Percentual de ferrugem |
|-------------------|------------------------|
| Saudável          | até 4%                 |
| Merece observação | acima de 4% até 30%    |
| Doente            | acima de 30%           |

Saída: área da folha, área com ferrugem (em pixels), percentual e classificação. Na imagem,
a folha aparece contornada em azul e a ferrugem em magenta.

Resultado nas imagens do enunciado: 0,04% (Saudável), 8,8% (Merece observação) e 56,1%
(Doente).

## Base comum

Classes de apoio do pacote:

| Classe      | Responsabilidade                                                                    |
|-------------|-------------------------------------------------------------------------------------|
| `BinaryOps` | tons de cinza, gaussiana 5x5, Otsu, Sobel, black-hat, erosão/dilatação/abertura/fechamento com elemento quadrado, preenchimento de buracos, mediana da moldura |
| `Region`    | componente conexo com máscara local: área, centroide, retângulo envolvente, furos, perímetro, circularidade, casco convexo, solidez e retângulo de área mínima |
| `Ordering`  | ordena regiões em ordem de leitura (agrupa por linha e depois por x)                |
| `Annotator` | desenha contornos, preenchimentos translúcidos e rótulos sobre a imagem             |
| `WorkingImage` | reduz a imagem para a escala de trabalho e converte as medidas de volta          |

Perímetro: o contorno é seguido pelo algoritmo de Moore (vizinhança 8) e medido pelo código
de cadeia, com passo 1 na horizontal e vertical e √2 na diagonal. O contorno passa pelo
centro dos pixels de borda; um quadrado de lado `n` mede `4(n-1)`.

Casco convexo: algoritmo de Andrew (monotone chain) sobre os cantos dos pixels de borda, com
área pela fórmula do laço (shoelace).
