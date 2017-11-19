 --- Window Shopping ---

Predict the next item a player will buy in DOTA2.

To start use the command "java -jar WindowShopping.jar" in your shell, it will then load the training data from the folder the jar file is in which should contain "players.csv" and "purchase_log.csv". After the training is done it will write the first expected purchase (an integer item id) to the standard output. You can then use the commands '+<ID>' ex. +65 to add an item to the sequence of purchases, '-reset' to clear the sequence, and '-exit' to exit the program. After each change to the sequence the next expected purchase will be written to the standard output.

It's written in Java and uses a library called SPMF (http://www.philippe-fournier-viger.com/spmf/index.php) which has an implementation of the All-k Order Markov sequence prediction model found in this paper:

Pitkow, J., Pirolli, P.: Mining longest repeating subsequence to predict world wide web surng. In: Proc. 2nd USENIX Symposium on Internet Technologies and Systems, Boulder, CO, pp. 13–25 (1999)

The library is modified a bit to improve performance and add small features.