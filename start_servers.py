import shutil
import subprocess
import psutil
import requests
import time
from random import randint
from kazoo.client import KazooClient
from kazoo.retry import KazooRetry
from requests import RequestException, HTTPError

MAX_SERVERS = 100
SHARD_COUNT = 1;
SERVERS_PER_SHARD = 3;
START_PORT_REST = 9000
START_PORT_GRPC = 10000
IP_ADDRESS = "127.0.0.1"
ZOOKEEPER_PORT = 2181
ZOOKEEPER_HOSTPORT = IP_ADDRESS + ":" + str(ZOOKEEPER_PORT)
TRANSACTIONS = "transactions"
CLIENTS = "clients"
ZOOKEEPER_SERVER_PATH = ".\\bin\\zkServer.cmd"
ZOOKEEPER_CLI_PATH = ".\\bin\\zkCli.cmd"
JAR_PATH = "java -jar build\\libs\\elections-final.jar"
COUNT_NOMINEES = 0

def rest_uri(server_number, nominee, voter_id):
    return "http://" + IP_ADDRESS + ":" + str(START_PORT_REST + server_number) + "/" + str(nominee) + "/" + str(
        voter_id)


def _get_command_type_and_args():
    return input("\nEnter command type: ").lower().strip()


def _add_zeros_to_args_list(args_list):
    return args_list + ['0' for i in range(len(args_list), 4)]


def elections_start(server_number='0', voter_id='0', nominee_id='0'):
    if server_number != '0' or voter_id != '0' or nominee_id != '0':
        print("voter_id and nominee_id should be 0")
        return

    uri = "http://" + IP_ADDRESS + ":" + str(START_PORT_REST + number_of_servers) + "/start"
    try:
        response = requests.post(uri)
    except RequestException or HTTPError:  # retry on error
        print("error")
        return
    print(response.text)


def elections_end(server_number='0', voter_id='0', nominee_id='0'):
    if server_number != '0' or voter_id != '0' or nominee_id != '0':
        print("voter_id and nominee_id should be 0")
        return

    uri = "http://" + IP_ADDRESS + ":" + str(START_PORT_REST + number_of_servers) + "/end"
    try:
        response = requests.post(uri)
    except RequestException or HTTPError:  # retry on error
        return
    print(response.text)


def view_report_shard(server_number='0', voter_id='0', nominee_id='0'):
    if server_number != '0' or voter_id != '0' or nominee_id != '0':
        print("voter_id and nominee_id should be 0")
        return

    shard_number = int(input("  Enter the state of which to view report of: "))
    uri = "http://" + IP_ADDRESS + ":" + str(START_PORT_REST + number_of_servers) + "/report/" + str(shard_number)
    try:
        response = requests.put(uri)
    except RequestException or HTTPError:  # retry on error
        return
    print(response.text)

def view_report(server_number='0', voter_id='0', nominee_id='0'):
    if server_number != '0' or voter_id != '0' or nominee_id != '0':
        print("voter_id and nominee_id should be 0")
        return

    uri = "http://" + IP_ADDRESS + ":" + str(START_PORT_REST + number_of_servers) + "/report"
    try:
        response = requests.put(uri)
    except RequestException or HTTPError:  # retry on error
        print("Error")
        return
    print(response.text)

def exit_(server_number='0', voter_id='0', nominee_id='0'):
    # kill_proc(zk_cli_proc)

    for p in servers_proc.values():
        kill_proc(p)

    kill_proc(zk_server_proc)

    exit(0)


def add_new_server(server_number, voter_id='0', nominee_id='0'):
    if voter_id != '0' or nominee_id != '0':
        print("voter_id and nominee_id should be 0")
        return

    if servers_proc.__contains__(server_number):
        try:
            psutil.Process(servers_proc[server_number].pid)
            print("Server number " + str(server_number) + " is already open")
            return
        except psutil.NoSuchProcess:
            servers_proc.pop(server_number)
            return add_new_server(server_number)
        except KeyError:
            pass

    if len(servers_proc) >= MAX_SERVERS:
        print("You have reached the maximum servers number")
        return

    rest_port = str(START_PORT_REST + server_number)
    grpc_port = str(START_PORT_GRPC + server_number)
    grpc_address = IP_ADDRESS + ":" + grpc_port
    shard_name = str(int(server_number / number_of_servers_per_shard))

    jar_args = rest_port + " " + grpc_address + " " + ZOOKEEPER_HOSTPORT + " " + str(number_of_shards) + " " + str(
        number_of_servers_per_shard) + " " + shard_name
    run_command = JAR_PATH + " " + jar_args
    sever_proc = subprocess.Popen(run_command, creationflags=subprocess.CREATE_NEW_CONSOLE)
    print(run_command)
    print("Opened server number " + str(server_number))

    servers_proc[server_number] = sever_proc


def close_server(server_number='0', voter_id='0', nominee_id='0'):
    if voter_id != '0' or nominee_id != '0':
        print("voter_id and nominee_id should be 0")
        return

    if server_number == '0':
        server_number = input("Enter server number: ")

    server_number = int(server_number)

    try:
        psutil.Process(servers_proc[server_number].pid)
    except (psutil.NoSuchProcess, KeyError):
        servers_proc.pop(server_number, '0')
        print("Server number " + str(server_number) + " isn't running")
        return
    else:
        kill_proc(servers_proc[server_number])
        servers_proc.pop(server_number)


def vote(server_number='0', voter_id='0', nominee_id='0'):
    if server_number == '0' or voter_id == '0' or nominee_id == '0':
        server_number = input("    Enter server number: ")
        voter_id = input("    Enter voter id: ")
        nominee_id = input("    Enter nominee_id: ")

    server_number = int(server_number)
    voter_id = int(voter_id)
    nominee_id = int(nominee_id)

    try:
        psutil.Process(servers_proc[server_number].pid)
    except (psutil.NoSuchProcess, KeyError):
        servers_proc.pop(server_number, '0')
        print("Server number " + str(server_number) + " isn't running")
        return

    uri = rest_uri(server_number, nominee_id, voter_id)
    try:
        response = requests.put(uri)
    except RequestException or HTTPError:  # retry on error
        return

    print("\nserver number " + str(server_number) + " response code is " + str(response.status_code))
    print(response.text)


def test(server_number='0', voter_id='0', nominee_id='0'):
    if voter_id != '0' or nominee_id != '0':
        print("voter_id and nominee_id should be 0")
        return

    if server_number == '0':
        vote_count = int(input("    Enter vote count: "))
        number_of_voters = int(input("    Enter the number of legitimate voters to test: "))

    final_vote_count = 0
    for i in range(vote_count):
        server_id = randint(0, number_of_servers - 1)

        server_number = int(server_id)
        try:
            psutil.Process(servers_proc[server_number].pid)
        except (psutil.NoSuchProcess, KeyError):
            servers_proc.pop(server_number, '0')
            print("Server number " + str(server_number) + " isn't running")
            continue

        voter_id = randint(0, number_of_voters - 1)
        nominee_id = randint(0, COUNT_NOMINEES - 1)
        print("voter " + str(voter_id) + " voted to candidate: " + str(nominee_id))
        uri = rest_uri(server_id, nominee_id, voter_id)
        try:
            response = requests.put(uri)
            final_vote_count = final_vote_count + 1
        except RequestException or HTTPError:  # retry on error
            continue

    print("Sent " + str(final_vote_count) + " vote requests")


def kill_proc(proc):
    try:
        p = psutil.Process(proc.pid)
        # list children & kill them
        for c in p.children(recursive=True):
            c.kill()
        p.kill()
    except (psutil.NoSuchProcess, KeyError):
        pass


if __name__ == "__main__":

    shutil.rmtree('./tmp/', ignore_errors=True)
    shutil.rmtree('./zookeeper_data', ignore_errors=True)

    current_server_number = 0

    servers_proc = {}

    main_script = int(input("     Are you the main script? 1 for yes, 0 for no "))

    if main_script == 1:
        zk_server_proc = subprocess.Popen(ZOOKEEPER_SERVER_PATH, creationflags=subprocess.CREATE_NEW_CONSOLE)
        input("Press Enter when zookeeper server is running.")
        # time.sleep(10)

        retry = KazooRetry(max_tries=-1, max_delay=5)
        zk = KazooClient(hosts=ZOOKEEPER_HOSTPORT)
        result = retry(zk.start)
        # zk.ensure_path("/elections")
        zk.stop()

        # zk_cli_proc = subprocess.Popen(
        #     ZOOKEEPER_CLI_PATH, creationflags=subprocess.CREATE_NEW_CONSOLE)
        # input("Press Enter when zookeeper client is running")

        number_of_shards = int(input("Enter number of states: "))
        number_of_servers_per_shard = int(input("Enter number of servers per state: "))
        number_of_servers = number_of_servers_per_shard * number_of_shards
        COUNT_NOMINEES = len(open("nominees.txt").readlines(  )) - 1

        if number_of_servers > MAX_SERVERS:
            raise Exception('Number of server is ' + str(number_of_servers) +
                            ' but the maximum is ' + str(MAX_SERVERS))

        zookeeper_address = IP_ADDRESS

        for i in range(0, number_of_servers + 1):
            add_new_server(current_server_number)
            current_server_number += 1

        input("Press Enter when the servers are running")

        commands = {"vote": vote, "kill": close_server, "start": elections_start,
                    "end": elections_end, "report": view_report, "sreport": view_report_shard,
                    "exit": exit_, "test": test}

    else:
        number_of_servers = int(input("Enter number of servers: "))
        commands = {"sreport": view_report_shard, "report": view_report}

    while True:
        user_input = _get_command_type_and_args()
        user_input = user_input.split()

        user_command = user_input[0]
        user_args = _add_zeros_to_args_list(user_input[1:])

        if commands.__contains__(user_command):
            commands[user_command](
                user_args[0], user_args[1], user_args[2])
        else:
            print("Unsupported command type. Sported types are:")
            print("vote, kill, start, end, report, sreport(shard report), exit, test")
