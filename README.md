questgiver
===

Uma _to-do list_ para Android. Concebido em meados de 2013 como um projeto 
pessoal para aprendizado, e meu primeiro contato com java.

**Recursos**

 - Duas abas principais, uma onde se guardam as tarefas em uma estrutura de
   árvore, outra onde um resumo das tarefas mais importantes é mostrado a partir
   de uma regra de pontuação;
   
 - Indicador de performance de 0 a 100;
   
 - As tarefas podem ser "simples" ou "semanais/recorrentes", cada uma possuindo prazo e importância;
 
 - Notificações em eventos como expiração e resumo diário;
 
 
**Roadmap**

Depois de alguns anos de abandono, pretendo anterar o indicador de performance, colocando um gráfico
de histórico de pontos. Também pretendo criar uma widget pra colocar na área de trabalho.

**Build**

Deve funcionar OK com Android Studio 2.x. No 1.x certamente não compilará de primeira devido
à versão diferente da estrutura do Gradle.

Inclui uma versão modificada do [spinner wheel](https://github.com/ai212983/android-spinnerwheel).